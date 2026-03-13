//$Id$
package org.owasp.validator.css;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.owasp.validator.html.InternalPolicy;
import org.owasp.validator.html.model.Property;
import org.owasp.validator.html.util.ErrorMessageUtil;
import org.owasp.validator.html.util.HTMLEntityEncoder;
import org.w3c.dom.css.CSSValue;
import org.w3c.dom.css.CSSValueList;

import com.steadystate.css.dom.CSSValueImpl;
import com.steadystate.css.dom.DOMExceptionImpl;


/**
 * Encapsulates parsing and validation of @keyframe style declaration.
 * This class is instantiated in the ZohoCssValidator with the
 * desired policy 
 * @see #getKeyFrameRuleMap(String)
 * @see #getKeyFrameCSSRules(String)
 * @see #getKeyFrameInnerCSSRules(String)
 * @see #getvalidatedKeyFramePropertyValue(String, Property)
 * 
 * IMPORTANT NOTE :: 
 *  1. Existing antisamy-1.5.3 uses batik-css to parse styles which doesn't CSS3. 
 *  	The behaviour of antisamy-1.5.3 is discussed @ https://intranet.wiki.zoho.com/security/Antisamy---CSS3behaviour.html
 *  2. Since there is no development in both antisamy and batik-css, we removed the dependency of batik-css engine 
 *     instead used css-parser-0.9.20 (From M2_11) to parse styles in Antisamy
 *  3. This class should be bundled with antisamy-1.5.3.jar 
 */
public class ZohoKeyFrameRuleImp {
	ZohoCssValidator cssValidator;
    public ZohoKeyFrameRuleImp(ZohoCssValidator zohoCssValidator) {
	   cssValidator = zohoCssValidator;
	}

/**
 * Return keyFrameRuleMap
 * @param unknownRuleText
 * @return
 */
    Map<String, String> getKeyFrameRuleMap(String unknownRuleText) {
		Map<String,String> keyFrameRuleMap = new LinkedHashMap<String, String>();
		String keyFrameCSSRules = getKeyFrameCSSRules(unknownRuleText);
		while (keyFrameCSSRules.length() >0){
			String innerselector = cssValidator.getKeyFrameSelectorString(keyFrameCSSRules);
			if(innerselector.length() > 0) {
			   keyFrameCSSRules = keyFrameCSSRules.substring(keyFrameCSSRules.indexOf(innerselector)+innerselector.length());
				String  innercssrules = getKeyFrameInnerCSSRules(keyFrameCSSRules);
				if(innercssrules.trim().length() > 0){
					keyFrameRuleMap.put(innerselector, innercssrules.trim());
					/* NOTE : here +1 in substring will not throw any ArrayIndexOutofBoundException , since we are getting
					 *  substring between { } (open and close curly braces) in "innercssrules"
					 */
					keyFrameCSSRules = keyFrameCSSRules.substring(keyFrameCSSRules.indexOf(innercssrules)+innercssrules.length()+1);
				 }else {
					 break;
				 }
			}else {
				break;
			}
		}
		return keyFrameRuleMap;
	}

    /**
     * Returns the CssRules within @keyFrames { }  
     * Example : @keyframes blinker { 0% { opacity: 1.0; } 50% { opacity: 0.0; } 100% { opacity: 1.0; } }
     * @param kframeCSSString
     * @return
     */
    
	private static String getKeyFrameCSSRules(String kframeCSSString) {
		if(kframeCSSString.indexOf("{") != -1 && kframeCSSString.lastIndexOf("}") != -1){
			kframeCSSString = kframeCSSString.substring(kframeCSSString.indexOf("{") + 1,kframeCSSString.lastIndexOf("}"));
		    return kframeCSSString.trim();
		}
		return "";
	}

	/**
	 * Returns Inner Cssrules
	 * Example :  0% { opacity: 1.0; color : red; }  which return opacity: 1.0; color : red;
	 * @param kframeInnerCSSRules
	 * @return
	 */
    
	private static String getKeyFrameInnerCSSRules(String kframeInnerCSSRules) {
		if(kframeInnerCSSRules.indexOf("{") != -1 && kframeInnerCSSRules.indexOf("}") != -1){
		   kframeInnerCSSRules = kframeInnerCSSRules.substring(kframeInnerCSSRules.indexOf("{") + 1, kframeInnerCSSRules.indexOf("}"));	
		   return kframeInnerCSSRules;
		}
		return "";
	}

	/**
	 * Validated CSS Rules against antisamy policy file 
	 * @param keyFrameRuleMap
	 * @param errorMessages 
	 * @return
	 */
   String getValidatedKeyFrameRule(Map<String, String> keyFrameRuleMap, List<String> errorMessages) {
		StringBuilder innerCSSRuleBuilder = new StringBuilder();
		for(Entry<String, String> cssRules : keyFrameRuleMap.entrySet()){
			String selector = cssRules.getKey();
			if(cssValidator.isValidSimpleSelectorName(selector, "keyFrameInnerSelector")){
				String css = cssRules.getValue();
				String[] cssPropertyArray = css.split(";");
				StringBuilder innerCSSRulePropertyBuilder = new StringBuilder();
				for (String cssProperty : cssPropertyArray){
					if(cssProperty.contains(":")){
						// if cssProperty contains ":" , then its not possible to get <2 elements in an array, hence the array cssProp[] is accessed by index directly
						String[]cssProp = cssProperty.split(":", 2);
						String propName  = cssProp[0].trim();
						Property antisamyPropertyRule = cssValidator.getPolicy().getPropertyByName(propName.toLowerCase(Locale.ROOT));   
						if (antisamyPropertyRule != null) {	   
							try{
							 String validatePropertyValue = getvalidatedKeyFramePropertyValue(cssProp[1].trim(),antisamyPropertyRule);	
							 	if(validatePropertyValue.length() > 0 ){
						    		innerCSSRulePropertyBuilder.append("    "+propName+" : "+validatePropertyValue+";\n");
						    	 }
							}catch(DOMExceptionImpl e){
								cssValidator.addError(errorMessages, ErrorMessageUtil.ERROR_CSS_KEYFRAME_PROPERTY_INVALID, new Object[]{HTMLEntityEncoder.htmlEntityEncode(propName)});
			             	}
						}
					}
				}
				if(innerCSSRulePropertyBuilder.length()>0){
					innerCSSRuleBuilder.append(selector+"{\n"); //No i18N
					innerCSSRuleBuilder.append(innerCSSRulePropertyBuilder+"}\n"); //No i18N
				}
			}
		}
		return innerCSSRuleBuilder.toString();
	}
	
   /**
    * Return validated property value
    * @param value
    * @param antisamyPropertyRule
    * @return
    */
	private String getvalidatedKeyFramePropertyValue(String value, Property antisamyPropertyRule) {
		CSSValueImpl cssValue = new CSSValueImpl();
		cssValue.setCssText(value);
		StringBuilder cssvalueBuilder = new StringBuilder();
		if( cssValue.getCssValueType() == CSSValue.CSS_VALUE_LIST){
			CSSValueList cssValList = (CSSValueList)cssValue;
			
			for (int n= 0; n < cssValList.getLength(); n++) {
				CSSValueImpl cssSubValue = (CSSValueImpl) cssValList.item(n);
				validateKeyFramePropertyValue(cssSubValue,antisamyPropertyRule,cssvalueBuilder);					    				
			}			
		}else {
			validateKeyFramePropertyValue(cssValue,antisamyPropertyRule,cssvalueBuilder);	
		}
		return cssvalueBuilder.toString();
	}

/**
 * adds validated property value to StringBuilder
 * @param cssValue
 * @param antisamyPropertyRule
 * @param cssvalueBuilder
 */
private void validateKeyFramePropertyValue(CSSValueImpl cssValue, Property antisamyPropertyRule, StringBuilder cssvalueBuilder) {
	String propValue = cssValue.getCssText();
	if(cssValue.getPrimitiveType() == ZohoCssValidator.CSS_RGBCOLOR ){
		propValue = propValue.replaceAll("\\s", "");	//No i18N						    					
	  }
	if(cssValidator.isValidValue(antisamyPropertyRule, propValue)){
			cssvalueBuilder.append(propValue+" ");
	  }				
}

}
