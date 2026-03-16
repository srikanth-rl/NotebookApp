//$Id$
package org.owasp.validator.css;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.InternalPolicy;
import org.owasp.validator.html.ScanException;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.w3c.dom.css.CSSStyleSheet;

import com.steadystate.css.dom.CSSStyleDeclarationImpl;
import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;

/**
 * Encapsulates the parsing and validation of a internal or inline style declaration. 
 * To make use of this class, instantiate the scanner with the
 * desired policy and call either <code>scanInlineSheet()</code> or
 * <code>scanStyleSheet</code> as appropriate.
 *
 * 
 * IMPORTANT NOTE :: 
 *  1. Existing antisamy-1.5.3 uses batik-css to parse styles which doesn't CSS3. 
 *  	The behaviour of antisamy-1.5.3 is discussed @ https://intranet.wiki.zoho.com/security/Antisamy---CSS3behaviour.html
 *  2. Since there is no development in both antisamy and batik-css, we removed the dependency of batik-css engine 
 *     instead used css-parser-0.9.20 (From M2_11) to parse styles in Antisamy
 *  3. This class should be bundled with antisamy-1.5.3.jar instead of existing CssScanner.class 
 */

public class ZohoCssScanner {
	private static final Logger LOGGER = Logger.getLogger(ZohoCssScanner.class.getName());
	protected final InternalPolicy policy;
	private static final String CDATA = "^\\s*<!\\[CDATA\\[(.*)\\]\\]>\\s*$";  //No i18N
	CSSOMParser parser ;
	ZohoCssValidator cssValidator;
	boolean removeImportRule = true;
	private static final Pattern CDATA_PATTERN = Pattern.compile(CDATA, Pattern.DOTALL);
	
	public ZohoCssScanner(InternalPolicy policy, ResourceBundle messages) {
		this.policy = policy;		
		// CSS3 Parser instantiation
    	this.parser = new CSSOMParser(new SACParserCSS3());
    	// Error handler is set to print simplified error messages
    	this.parser.setErrorHandler(new CSSErrorHandler());
    	// CSS validator instantiation
    	this.cssValidator = new ZohoCssValidator(policy,messages);
	}


/**
 * Parse and validate CSS StyleSheet
 * @param taintedCss
 * @return CleanResults
 * @throws ScanException
 */
	public CleanResults scanStyleSheet(String taintedCss) throws ScanException {
		
		long startOfScan = System.currentTimeMillis();
		CleanResults cleanCSS;
	    /* Check to see if the text starts with (\s)*<![CDATA[
		 * and end with ]]>(\s)*.
		 */

	    Matcher m = CDATA_PATTERN.matcher(taintedCss);
		
		boolean isCdata = m.matches();	
	
		if ( isCdata ) {
			taintedCss = m.group(1);
		}
		
		/* The style rules like below contains 'unicode' space  [ctrl + shift + u , type 00A0 which generates unicode space ]  
		 * Example : html { order: 7; width  0px;}
		 * It looks like valid css but don't render in browser
		 * To avoid unnecessary supports regarding these type of css , unicode space is removed before parsing the styles
		 */
        taintedCss= removeUnicodeSpace(taintedCss);
		InputSource is = new InputSource(new StringReader(taintedCss));
		try
		{
			long startTime = System.currentTimeMillis();
			CSSStyleSheet stylesheet = parser.parseStyleSheet(is,null,null);
			long totalTime = (System.currentTimeMillis() - startTime);
			if(totalTime > 5000) {
				LOGGER.log(Level.INFO, "Style sheet scanning time in milli second: ", new Object[]{totalTime});
			}
			
			startTime = System.currentTimeMillis();
			cleanCSS= cssValidator.validateStyleSheet(stylesheet,isCdata,startOfScan);
			totalTime = (System.currentTimeMillis() - startTime);
			if(totalTime > 5000) {
				LOGGER.log(Level.INFO, "Style sheet validation time in milli second: ", new Object[]{totalTime});
			}
		}
		catch(IOException e){
			throw new ScanException(e);
		}
	   return cleanCSS;
	}

	private String removeUnicodeSpace(String str) {
	return str.replace("\u00A0",""); //No i18N
}


	/**
	 * Parse and validate InlineSyles
	 * @param attrvalue
	 * @return
	 * @throws ScanException
	 */
	public CleanResults scanInlineStyle(String attrvalue) throws ScanException {
		long startOfScan = System.currentTimeMillis();
		CleanResults cleanedCSSResult;
		List<String> errorMessages = new ArrayList<String>();
		attrvalue= removeUnicodeSpace(attrvalue.trim());
		InputSource is = new InputSource(new StringReader(attrvalue));
		try{
			long startTime = System.currentTimeMillis();
		    CSSStyleDeclaration propertiesList = parser.parseStyleDeclaration(is);
		    long totalTime = (System.currentTimeMillis() - startTime);
		    if(totalTime > 5000) {
				LOGGER.log(Level.INFO, "Inline style scanning time in milli second: ", new Object[]{totalTime});
			}
			//CSSStyleDeclarationImpl is used to make use of cssProperties.getProperties() API
			CSSStyleDeclarationImpl cssProperties = (CSSStyleDeclarationImpl) propertiesList;
		    startTime = System.currentTimeMillis();
			cleanedCSSResult= cssValidator.validateInlineStyles(startOfScan,cssProperties,errorMessages);
			totalTime = System.currentTimeMillis() - startTime;
			if(totalTime > 5000) {
				LOGGER.log(Level.INFO, "Inline style validation time in milli second: ", new Object[]{totalTime});
			}
		}catch(IOException e){
			throw new ScanException(e);
		}
		return cleanedCSSResult;
	}

}

/*
 * CSSErrorHandler is used to print simplified error messages when CSSParseException is thrown from com.steadystate.css.parser
 * 	Sample error message thrown by default 
 * 		 - org.w3c.css.sac.CSSParseException: Error in expression. (Invalid token ":". Was expecting one of: <S>, <NUMBER>, "inherit", <IDENT>, <STRING>, "-", <PLUS>, <HASH>, <EMS>, <EXS>, <LENGTH_PX>, <LENGTH_CM>, <LENGTH_MM>, <LENGTH_IN>, <LENGTH_PT>, <LENGTH_PC>, <ANGLE_DEG>, <ANGLE_RAD>, <ANGLE_GRAD>, <TIME_MS>, <TIME_S>, <FREQ_HZ>, <FREQ_KHZ>, <RESOLUTION_DPI>, <RESOLUTION_DPCM>, <PERCENTAGE>, <DIMENSION>, <URI>, <FUNCTION>, "progid:".) 
 *  Simplified error message to be printed by our CSSErrorHandler
 *   	-  CSS Syntax Error at  [ line - 3 : column - 8]
 *   
 *  IMPORTANT NOTE :
 *     Throw exception; should not be used in CSSErrorHandler which will truncate entire parsed stylesheet 
 *     instead of skipping particular block which cause ParseException.
 */
   class CSSErrorHandler implements ErrorHandler {
	private static final Logger LOGGER = Logger.getLogger(CSSErrorHandler.class.getName());
		
	@Override
	public void error(CSSParseException exception) throws CSSException {
          LOGGER.log(Level.SEVERE, "CSS Syntax Error at [ line - "+exception.getLineNumber()+" : column - "+exception.getColumnNumber()+" ]");
	}

	@Override
	public void fatalError(CSSParseException exception) throws CSSException {
       LOGGER.log(Level.SEVERE, "CSS Syntax Fatal Error at [ line - "+exception.getLineNumber()+" : column - "+exception.getColumnNumber()+" ]");
	}

	@Override
	public void warning(CSSParseException exception) throws CSSException {
	    LOGGER.log(Level.INFO, "CSS Syntax Warning at [ line - "+exception.getLineNumber()+" : column - "+exception.getColumnNumber()+" ]");

	}

}
