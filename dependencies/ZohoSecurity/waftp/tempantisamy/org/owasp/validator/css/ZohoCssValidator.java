//$Id$
package org.owasp.validator.css;
/**
 * 
 * @author asiya-2559
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.Locale;

import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.InternalPolicy;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.model.AntiSamyPattern;
import org.owasp.validator.html.model.Property;
import org.owasp.validator.html.util.ErrorMessageUtil;
import org.owasp.validator.html.util.HTMLEntityEncoder;
import org.w3c.dom.css.CSSCharsetRule;
import org.w3c.dom.css.CSSFontFaceRule;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSMediaRule;
import org.w3c.dom.css.CSSPageRule;
import org.w3c.dom.css.CSSRule;
import org.w3c.dom.css.CSSRuleList;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleRule;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.css.CSSUnknownRule;
import org.w3c.dom.css.CSSValue;
import org.w3c.dom.css.CSSValueList;

import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.dom.CSSStyleSheetImpl;
import com.steadystate.css.dom.CSSValueImpl;
import com.steadystate.css.dom.DOMExceptionImpl;
import com.steadystate.css.dom.MediaListImpl;
import com.steadystate.css.format.CSSFormat;
import com.steadystate.css.parser.media.MediaQuery;


/**
 * Encapsulates validation of a internal or inline style declaration.
 * To make use of this class, instantiate the scanner with the
 * desired policy and call either <code>validateStyleSheet()</code> or
 * <code>validateInlineStyles</code> as appropriate.
 * 
 * @see #validateStyleSheet(CSSStyleSheet, boolean, long)
 * @see #validateInlineStyles(long, CSSStyleDeclarationImpl, List)
 * 
 * IMPORTANT NOTE :: 
 *  1. Existing antisamy-1.5.3 uses batik-css to parse styles which doesn't CSS3. 
 *  	The behaviour of antisamy-1.5.3 is discussed @ https://intranet.wiki.zoho.com/security/Antisamy---CSS3behaviour.html
 *  2. Since there is no development in both antisamy and batik-css, we removed the dependency of batik-css engine 
 *     instead used css-parser-0.9.20 (From M2_11) to parse styles in Antisamy
 *  3. This class should be bundled with antisamy-1.5.3.jar instead of existing CssValidator.class 
 */
public class ZohoCssValidator {
	private static final Logger LOGGER = Logger.getLogger(ZohoCssValidator.class.getName());
	
	protected static final short CSS_RGBCOLOR = 25;
	protected static final List<String> DEFAULT_CSS_SEPARATOR = Arrays.asList(",", "/"); //No I18N
	protected final List<String> cssSeparator;
	protected final InternalPolicy policy;
	CSSFormat cssFormatter;
	CSSFormat cssInlineStyleFormatter;
	protected final ResourceBundle messages;
	
	/*To be discussed, 
	 * https://asiya-a-2559.wiki.zoho.com/AntisamyHTML5AndCSS3Support/CSS2-and-CSS3-Properites.html
	 * In new implementation, we are validating all the selectors with the single regex "cssSelectorCommonRegex" which is added 
	 *                        in antisamy-myspace-1.4.4.xml
	 * If the regex is not configured in antisamy-policy.xml, the selector validation gets failed (null pointer exception wiil be thrown)
	 * This will cause the migration for the service teams who all are using custom-antisamypolicy file
	 * Hence , default values are hardcoded
	 * */
	protected final AntiSamyPattern cssSelectorDefaultRegex = new AntiSamyPattern(Pattern.compile("[a-zA-Z0-9\\-_\"\\[\\]\\|\\s.*+,>#%:=~^$()\\P{InBasicLatin}]+"));
	protected final AntiSamyPattern cssSelectorDefaultRegexExclusion =  new AntiSamyPattern(Pattern.compile(""));
	protected final AntiSamyPattern cssAtCharsetDefaultEncoding =  new AntiSamyPattern(Pattern.compile("(utf-8|iso-8859-15)"));
   
	protected ZohoCssValidator(InternalPolicy policy, ResourceBundle messages) {
		this.policy = policy;
		this.messages = messages;
		String cssSeparatorList = policy.getDirective("cssSeparatorList"); //No I18N
		boolean includeCssSpace = policy.isIncludeCssSpace();
		boolean formatOutputCSS = policy.isFormatOutput();
		this.cssSeparator = isValid(cssSeparatorList) ?  Arrays.asList(cssSeparatorList.split("\\|")) : DEFAULT_CSS_SEPARATOR;
		this.cssFormatter = new CSSFormat();
		this.cssInlineStyleFormatter = new CSSFormat();
		this.cssInlineStyleFormatter = cssInlineStyleFormatter.setIncludeCssSpace(includeCssSpace);
		/* setCSSOutputFormatter();  is used to get each css properties in newline with tab indent(4 spaces)
		 * If this is not set , then all properties will be displayed in single line
		 */
    	setCSSOutputFormatter(formatOutputCSS);
	}
	
	private void setCSSOutputFormatter(boolean formatOutputCSS) {
		
		if(formatOutputCSS){
			cssFormatter.setPropertiesInSeparateLines(4);
		}
		else{
			cssFormatter.setPropertiesInSeparateLines(-1);
		}
	}
	
  
	/**
     * Validates the CSSStyles based on the types(@media , @page, @charset, @fontface, @import, simple styles)
     * @param stylesheet
     * @param isCdata 
     * @param startOfScan
     * @return 
     */
    
    public CleanResults validateStyleSheet(CSSStyleSheet stylesheet, boolean isCdata, long startOfScan) {
        
    	String cleanedCSS= "";
    	List<String> errorMessages = new ArrayList<String>();
    	CSSRuleList rules = stylesheet.getCssRules();
	    for (int currentIndex = 0; currentIndex < rules.getLength(); currentIndex++) {
	    	CSSRule rule = rules.item(currentIndex);
	    	try{
	    	 if(rule.getType() == CSSRule.STYLE_RULE) {
	    		/*
	    		 * 	 p ~ ul { background: #ff0000; background-color:rgb(255,0,0);opacity:0.6;}
	    		 */
	 			CSSStyleRule styleRule = (CSSStyleRule)rule;
	 			if(!isValidSelectorName(toLowerCase(styleRule.getSelectorText()))){				
	 				addError(errorMessages, ErrorMessageUtil.ERROR_CSS_TAG_SELECTOR_DISALLOWED, new Object[]{HTMLEntityEncoder.htmlEntityEncode(styleRule.getSelectorText())});
	 				stylesheet.deleteRule(currentIndex);
	 				currentIndex --;
	 				continue;
	 			}
	 			CSSStyleDeclaration propertiesList= styleRule.getStyle();
	 			//CSSStyleDeclarationImpl is used to make use of cssProperties.getProperties() API		     
	 			CSSStyleDeclarationImpl cssProperties = (CSSStyleDeclarationImpl) propertiesList;
	 			List<com.steadystate.css.dom.Property> cssPropertiesList = cssProperties.getProperties();
	 			
	 			validateCSSProperties(cssPropertiesList,"",errorMessages);
	 		
	 		}else if(rule.getType()==CSSRule.CHARSET_RULE){
	 		   /*
	 		    * @charset "UTF-8"; 
	 		    */
	 			CSSCharsetRule charsetRule = (CSSCharsetRule)rule;
	 			String encoding = toLowerCase(charsetRule.getEncoding());
	 			if(!isRegexMatches(encoding,"cssAtCharsetEncoding",cssAtCharsetDefaultEncoding)){
	 				addError(errorMessages, ErrorMessageUtil.ERROR_CSS_CHARSET_ENCODING_INVALID, new Object[]{HTMLEntityEncoder.htmlEntityEncode(charsetRule.getEncoding())});
	 				stylesheet.deleteRule(currentIndex);
	 				currentIndex --;
	 				continue;
	 			}
	 	
	 		}else if(rule.getType()==CSSRule.FONT_FACE_RULE){
	 		   /*
	 		    * <style> @font-face {font-family: myFirstFont; src: url(sansation_bold.woff); font-weight: bold} </style>
	 		    */
	 			CSSFontFaceRule fontfaceRule = (CSSFontFaceRule)rule;
	 			CSSStyleDeclaration propertiesList= fontfaceRule.getStyle();
	 			
	 			//CSSStyleDeclarationImpl is used to make use of cssProperties.getProperties() API		     
	 			CSSStyleDeclarationImpl cssProperties = (CSSStyleDeclarationImpl) propertiesList;
	 			List<com.steadystate.css.dom.Property> cssPropertiesList = cssProperties.getProperties();
	 			
	 			validateCSSProperties(cssPropertiesList,"atfontface",errorMessages);		//No i18N			
	 		
	 		}else if(rule.getType()==CSSRule.MEDIA_RULE){
	 			/* Example @media : @media screen and (min-width: 480px) { body { background-color: lightgreen; }}
	 			 *   here : screen -> mediaType , (min-width : 480px) -> media properties
	 			 *          body { background-color: lightgreen; }  -> media css rules
	 			 */
	 			CSSMediaRule mediaRule = (CSSMediaRule)rule;
	 			
	 			//MediaListImpl is used to make use of mediaList.mediaQuery(index) API		      
	 			MediaListImpl mediaList = (MediaListImpl) mediaRule.getMedia();
	 			boolean isInvalid = false;
	 			if(mediaList.getLength() > 0){
	 			    for(int media_i=0; media_i<mediaList.getLength(); media_i++)		
	 				{
	 				MediaQuery mediaquery = mediaList.mediaQuery(media_i);
	             	String mediaType=mediaquery.getMedia();
					if(!"".equals(mediaType) && !isRegexMatches(mediaType,"mediaType",null)){
	             		addError(errorMessages, ErrorMessageUtil.ERROR_CSS_MEDIATYPE_INVALID, new Object[]{HTMLEntityEncoder.htmlEntityEncode(toLowerCase(mediaType))});
	             		stylesheet.deleteRule(currentIndex);
	             		currentIndex --;
	             		isInvalid = true;
		 				break;
	             	}
	             	List<com.steadystate.css.dom.Property> mediaPropertiesList = mediaquery.getProperties();
	             	validateCSSProperties(mediaPropertiesList,"atmedia",errorMessages); //No i18N	             
	 			 }
	 			}
	 			if(isInvalid) {
	 				continue;
	 			}	
	 			CSSRuleList mediaStyleRules = mediaRule.getCssRules();
	 			for (int j = 0; j < mediaStyleRules.getLength(); j++) {
	 				CSSRule mediaStyle = mediaStyleRules.item(j);
	 				if(!isValidMediaChildRules(mediaStyle,errorMessages)){
	 					mediaRule.deleteRule(j);
	 					j--;
	 				}
	 			} 			
	 		}else if(rule.getType()==CSSRule.PAGE_RULE){
	 			/* Example- @page { size:8.5in 11in; margin: 2cm } */
	 			
	 			CSSPageRule pageRule = (CSSPageRule)rule;
	 			 String pageSelectorName = pageRule.getSelectorText();
	 			 if(!isValidSimpleSelectorName(pageSelectorName,"pageSelector")){	
	 				    addError(errorMessages, ErrorMessageUtil.ERROR_CSS_PAGE_SELECTOR_INVALID, new Object[]{HTMLEntityEncoder.htmlEntityEncode(pageSelectorName)});
	 					stylesheet.deleteRule(currentIndex);
	 					currentIndex --;
		 				continue;
	 				}
	 			CSSStyleDeclaration propertiesList= pageRule.getStyle();	 			
	 			
	 			//CSSStyleDeclarationImpl is used to make use of cssProperties.getProperties() API		     
	 			CSSStyleDeclarationImpl cssProperties = (CSSStyleDeclarationImpl) propertiesList;
	 			List<com.steadystate.css.dom.Property> cssPropertiesList = cssProperties.getProperties();	
	 			validateCSSProperties(cssPropertiesList,"paged",errorMessages); //No i18N
	 	      		
	 		}else if(rule.getType() == CSSRule.IMPORT_RULE) {
	 		    /* By Default : Import css is disabled and can be enabled by using  "allowAtImport" directive
	 		     * <style> @import url("stylesheetB.css");</style>
	 		     */
	 			CSSImportRule importRule = (CSSImportRule)rule;
	 	    	boolean allowAtImport = "true".equalsIgnoreCase(policy.getDirective("allowAtImport"));
	 	    	boolean isValidImportURL = isRegexMatches(importRule.getHref(),"onsiteURL",null) || isRegexMatches(importRule.getHref(),"offsiteURL",null);//No i18N
	 			if( !allowAtImport || !isValidImportURL){
	 				addError(errorMessages, ErrorMessageUtil.ERROR_CSS_IMPORT_URL_INVALID, new Object[]{HTMLEntityEncoder.htmlEntityEncode(importRule.getHref())});
	 				stylesheet.deleteRule(currentIndex);
	 				currentIndex --;
	 				continue;
	 			}
	 		}else if(rule.getType() == CSSRule.UNKNOWN_RULE){
	 			CSSUnknownRule unknownRule = (CSSUnknownRule)rule;
	 			String unknownRuleText = unknownRule.getCssText();
	 			StringBuilder keyFrameRuleBuilder = new StringBuilder();
	 			String validateKeyFrameCSSRule = null;
	 			if(isAtKeyFrameRule(unknownRuleText)){
	 				// Since @keyframe rules are yet to support in css-parser-0.9.21, we have writter our own keyframeRuleImpl parser to parse and validate @keyframe rules
	 				// This has to be changed once the css-parser-0.9.21 with @keyframes support is released https://sourceforge.net/p/cssparser/bugs/61/
	 				String keyFrameSelectorString = getKeyFrameSelectorString(unknownRuleText);
	 				if(keyFrameSelectorString.length()>0 && isValidSimpleSelectorName(keyFrameSelectorString, "unknownSelector")){
	 				  ZohoKeyFrameRuleImp zki = new ZohoKeyFrameRuleImp(this);
	 				  Map<String,String> keyFrameRuleMap =  zki.getKeyFrameRuleMap(unknownRuleText);
	 				  String validatedKeyFrameRule = zki.getValidatedKeyFrameRule(keyFrameRuleMap,errorMessages);
	 				  if(validatedKeyFrameRule.length() > 0) {
	 					    keyFrameRuleBuilder.append(keyFrameSelectorString+"{\n");  //No i18N
	 						keyFrameRuleBuilder.append(validatedKeyFrameRule.toString()+"\n}\n");  //No i18N
	 						validateKeyFrameCSSRule = keyFrameRuleBuilder.toString();
	 				  }
	 				}				
	 				stylesheet.deleteRule(currentIndex);
	 				if(validateKeyFrameCSSRule != null ){
	 					stylesheet.insertRule(validateKeyFrameCSSRule, currentIndex);
	 				}
	 				else{
	 					currentIndex --;
		 				continue;
	 				}
	 			}else{
	 				addError(errorMessages, ErrorMessageUtil.ERROR_CSS_UNKNOWNRULE, new Object[]{HTMLEntityEncoder.htmlEntityEncode(unknownRuleText)});
	 				stylesheet.deleteRule(currentIndex);
	 				currentIndex --;
	 				continue;
	 			}
	 			
	 		}	
	    	}catch(DOMExceptionImpl e){
	        	stylesheet.deleteRule(currentIndex);
	        	currentIndex --;
 				continue;
	    	}
	    }
	    //CSSStyleSheetImpl is used to make use of resultStyleSheet.getCssText() API		     		  
	    CSSStyleSheetImpl resultStyleSheet = (CSSStyleSheetImpl)stylesheet;	
	    cleanedCSS = resultStyleSheet.getCssText(cssFormatter).trim();
	    if(policy.isFormatOutput()){	
	    	cleanedCSS = cleanedCSS.length() > 0 ? "\n"+cleanedCSS+"\n" : cleanedCSS;  //No i18N
	    }else{
	    	cleanedCSS = cleanedCSS.length() > 0 ? ""+cleanedCSS+"" : cleanedCSS;  //No i18N
	    }
		
		if ( isCdata && cleanedCSS.trim().length()>0) {
			cleanedCSS = "<![CDATA[[" + cleanedCSS + "]]>"; //No i18N
		}
		return new CleanResults(startOfScan, cleanedCSS, null, errorMessages);
    }
	
   /**
    * Validates inline css 
    * @param startOfScan
    * @param cssProperties
    * @param errorMessages
    * @return
    */
    protected CleanResults validateInlineStyles(long startOfScan, CSSStyleDeclarationImpl cssProperties, List<String> errorMessages) {
    	List<com.steadystate.css.dom.Property> cssPropertiesList = cssProperties.getProperties();			
         validateCSSProperties(cssPropertiesList,"",errorMessages);
         String cleanedCSS =  ""+cssProperties.getCssText(this.cssInlineStyleFormatter); 	
         return new CleanResults(startOfScan, cleanedCSS, null, errorMessages);
    }

  /**
   * Validates CSS properties based on the category of css-rules mentioned in antisamy-policy file (atfontface, paged, atmedia, normal style ) 
   * @param cssPropertiesList
   * @param category
 * @param errorMessages 
   */
	protected  void validateCSSProperties(List<com.steadystate.css.dom.Property> cssPropertiesList, String category, List<String> errorMessages) {
		for (int m = 0; m < cssPropertiesList.size(); m++) {
			boolean isInvalidProperty = false;
			boolean preserveCssUrlQuotes = policy.isPreserveCssUrlQuotes();
		    com.steadystate.css.dom.Property cssProperty = cssPropertiesList.get(m);
		    String propName = cssProperty.getName();
		    Property antisamyPropertyRule = policy.getPropertyByName(toLowerCase(propName));
		    if(antisamyPropertyRule != null &&  (category.isEmpty() || antisamyPropertyRule.getCategoryValues().contains(toLowerCase(category)))) { // The property category is empty for normal styles
		    	//CSSValueImpl is used to make use of cssValue.getPrimitiveType() API in validateValue()		     
	  	   		 CSSValueImpl cssValue = (CSSValueImpl) cssProperty.getValue();
	    		 /* This check is added for @media with empty properties  Ex : @media (grid) { css...}
	    		  */
	    		 if(cssValue == null && "atmedia".equalsIgnoreCase(category)) {
	  	   			 if(!isRegexMatches(propName, "mediaFeatureWithEmptyValue", null)) {
	  	   				 isInvalidProperty = true; 
	  	   			 }
	  	   		 }else {
		  	   		if(cssValue.getCssValueType() == CSSValue.CSS_VALUE_LIST){
		    			CSSValueList cssValList = (CSSValueList)cssValue;
		    			for (int n= 0; n < cssValList.getLength(); n++) {
		    				CSSValueImpl cssSubValue = (CSSValueImpl) cssValList.item(n);
		    				if(!isValidCSSValue(cssSubValue, antisamyPropertyRule)) {
		    					isInvalidProperty = true;
		    					break;
		    				}
		    			}		    			
		    		}else {
		    			if(!isValidCSSValue(cssValue, antisamyPropertyRule)) {
		    				isInvalidProperty = true;
		    			}
		    		}
	  	   		 }	    
		    }else {
		    	isInvalidProperty = true;
		    }
		    if(isInvalidProperty) {
		    	cssPropertiesList.remove(m);
		    	m--;
		    }else if(preserveCssUrlQuotes){
				CSSValueImpl cssValueOverride = (CSSValueImpl) cssProperty.getValue();
				if(cssValueOverride.getValue().toString().startsWith("url(") && cssValueOverride.getValue().toString().endsWith(")")){
					cssValueOverride.setStringValue((short)20,"\""+cssValueOverride.getStringValue()+"\"");
					cssProperty.setValue(cssValueOverride);
					cssPropertiesList.remove(m);
					cssPropertiesList.add(m,cssProperty);
				}
			}
		}
   }
	
/**
 * Validate CSS Property Value
 * @param cssValue
 * @param antisamyPropertyRule
 */
	private boolean isValidCSSValue(CSSValueImpl cssValue, Property antisamyPropertyRule) {

		String propValue = toLowerCase(cssValue.getCssText());
		/*  cssparser format rgb function with space rgb(10 , 10 , 10) where as batik-css returns rgb(10,10,10)
		 */
		if(cssValue.getPrimitiveType() == CSS_RGBCOLOR ){
			propValue = propValue.replaceAll("\\s", ""); //No i18N
		}
		if (propValue.length() == 1 && cssSeparator.contains(propValue)) {
			return true;
		}
		return isValidValue(antisamyPropertyRule, propValue);	
		 
}

 /**
  * Validates styleRules under the @media rule 
  * @param mediaChildRule
 * @param errorMessages 
  * @return
  */
   private boolean isValidMediaChildRules(CSSRule mediaChildRule, List<String> errorMessages) {
		if(mediaChildRule.getType() == CSSRule.STYLE_RULE) {
			
			CSSStyleRule styleRule = (CSSStyleRule)mediaChildRule;
			if(!isValidSelectorName(toLowerCase(styleRule.getSelectorText()))){	
				addError(errorMessages, ErrorMessageUtil.ERROR_CSS_AT_MEDIA_SELECTOR_INVALID, new Object[]{HTMLEntityEncoder.htmlEntityEncode(styleRule.getSelectorText())});
				return false;
			}
			CSSStyleDeclaration propertiesList= styleRule.getStyle();
			//CSSStyleDeclarationImpl is used to make use of cssProperties.getProperties() API	
			CSSStyleDeclarationImpl cssProperties = (CSSStyleDeclarationImpl) propertiesList;
			List<com.steadystate.css.dom.Property> cssPropertiesList = cssProperties.getProperties();
			
			validateCSSProperties(cssPropertiesList,"",errorMessages);
			return true;
		
		}
		return false;
		
	}
   /**
    * Validates property value with antisamy css rules (referred validateValue() > CssValidator.java in antisamy)
    * @param property
    * @param propertyValue
    * @return
    */
	boolean isValidValue(Property property, String propertyValue) {
		// normalize the value to lowercase
		propertyValue = toLowerCase(propertyValue);

		// check if the value matches any of the allowed literal values
			for( String allowedValue:property.getAllowedValues()){
				if (allowedValue != null && allowedValue.equals(propertyValue)) {
					return true;
				}	
			  }
		// check if the value matches any of the allowed regular expressions
			for(Pattern pattern : property.getAllowedRegExp()){
				if (pattern != null && pattern.matcher(propertyValue).matches()) {
					return true;
				}
			  }
		// check if the value matches any of the allowed shorthands
			for(String shorthandRef : property.getShorthandRefs()){
				Property shorthandProperty = policy.getPropertyByName(shorthandRef);
				if (shorthandProperty != null && isValidValue(shorthandProperty, propertyValue)) {
					return true;
				  }
			   }
		
		return false;
	}

	/**
	 * Validates Selector Name
	 * @param selectorText
	 * @return
	 */
private boolean isValidSelectorName(String selectorText) {
 	return isRegexMatches(selectorText,"cssSelectorCommonRegex",cssSelectorDefaultRegex) //No i18N
		   && !isRegexMatches(selectorText,"cssSelectorCommonRegexExclusion",cssSelectorDefaultRegexExclusion); //No i18N
	}

/**
 * Validate  selector with simple alpha numeric regex
 * @param selectorName
 * @return
 */
	boolean isValidSimpleSelectorName(String selectorName,String policyRegexName) {
    	return (selectorName.isEmpty() ? true : isRegexMatches(selectorName, policyRegexName, null));		
	}

/**
 * returns true if regex  from policy/default regex matches 
 * @param text
 * @param policyRegexName
 * @param cssSelectorDefaultRegex
 * @return
 */
private boolean isRegexMatches(String text,String policyRegexName, AntiSamyPattern cssSelectorDefaultRegex) {	
	AntiSamyPattern regexPattern = policy.getCommonRegularExpressions(policyRegexName)  ;
	regexPattern = regexPattern == null ? cssSelectorDefaultRegex : regexPattern;	
	return (regexPattern == null ? false : regexPattern.matches(text));
	}

 
	/**
	 * Return true if @keyframes/@-webkit-keyframes/@-moz-keyframes are mentioned under <require-unknown-css-rules>
	 * @param unknownRuleText
	 * @return
	 */
private boolean isAtKeyFrameRule(String unknownRuleText) {
	List<String> atKeyFrameCSSSelector = policy.getAtKeyFrameCSSSelectorsList();
	for (String keyFrameSelector : atKeyFrameCSSSelector){
		 if (unknownRuleText.startsWith(keyFrameSelector)){			 
			 return true;
		 }
	}
	return false;
	}

/**
 * Returns selector of the style rule : Ex h1{ color : red } returns h1
 * @param kf css rules
 * @return
 */
String getKeyFrameSelectorString(String kf) {
	String kfSelectorString = kf.indexOf("{")!=-1 ? kf.substring(0,kf.indexOf("{")).trim() : "";
	return kfSelectorString;
}
 

/**
 * Returns policy 
 */
Policy getPolicy() {
	return policy;
 }

/**
 * add the error messages mentioned in AntiSamy_en_US.properties to the list 
 */
protected void addError(List<String> errorMessages,
		String errorKey, Object[] objects) {
               try{
		errorMessages.add(ErrorMessageUtil.getMessage(messages, errorKey, objects));
               }catch(MissingResourceException e){
	             // if any resource bundle exception is thrown 
                  LOGGER.log(Level.SEVERE, e.getMessage());
               }
    }
public boolean isValid(Object value) {
	return value != null && !value.equals("null") && !value.equals("");
}

/**
 * Duplicate of com.zoho.waf.util.StringUtil.toRootUpperCase
 *
 * @param source
 * @return
 */
public static String toUpperCase(final String source)
{
	return source == null ? null : source.toUpperCase(Locale.ROOT);
}

/**
 * Duplicate of com.zoho.waf.util.StringUtil.toLowerCase
 *
 * @param source
 * @return
 */

public static String toLowerCase(final String source)
{
	return source == null ? null : source.toLowerCase(Locale.ROOT);
}



}
