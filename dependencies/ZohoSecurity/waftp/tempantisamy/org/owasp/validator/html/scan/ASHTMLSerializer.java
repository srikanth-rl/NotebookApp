package org.owasp.validator.html.scan;

import java.io.IOException;
import java.io.Writer;
import org.apache.xml.serialize.ElementState;
import org.apache.xml.serialize.HTMLdtd;
import org.apache.xml.serialize.OutputFormat;
import org.owasp.validator.html.InternalPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.owasp.validator.html.TagMatcher;


@SuppressWarnings("deprecation")
public class ASHTMLSerializer extends org.apache.xml.serialize.HTMLSerializer {

  private static final Logger logger = LoggerFactory.getLogger(ASHTMLSerializer.class);
  private boolean encodeAllPossibleEntities;
private final TagMatcher requireClosingTags;
  private boolean encodeNonBreakingSpace = true;
  //ZOHO FIX to avoid html encoding for non HTML cases like "1 < 2"
  private boolean skipEscapeHtmlTags = false;

  public ASHTMLSerializer(Writer w, OutputFormat format, InternalPolicy policy) {
    super(w, format);
    this.encodeAllPossibleEntities = policy.isEntityEncodeIntlCharacters();
    this.requireClosingTags = policy.getRequiresClosingTags();
	  //ZOHO Directives
	  this.skipEscapeHtmlTags = "true".equalsIgnoreCase(policy.getDirective("skipEscapeHtmlTags"));
	  this.encodeNonBreakingSpace=policy.getDirective("encodeNonBreakingSpace")!=null? "true".equalsIgnoreCase(policy.getDirective("encodeNonBreakingSpace")) : true;
  }

  @Override
  protected String getEntityRef(int charToPrint) {
  //ZOHO FIX: Encoding the NO-BREAK SPACE (\u00A0) character
	//Reference https://code.google.com/p/owaspantisamy/issues/detail?id=162&can=1&q=nbsp
	if(charToPrint == '\u00A0' && this.encodeNonBreakingSpace) {
	  return "nbsp";
	}
	if(skipEscapeHtmlTags){ //ZOHO FIX to avoid html encoding for non HTML cases like "1 < 2"
      return null;
    }
    if (encodeAllPossibleEntities || Constants.big5CharsToEncode.indexOf(charToPrint) != -1)
      return super.getEntityRef(charToPrint);
    return null;
  }

  @Override
  public void endElementIO(String namespaceURI, String localName, String rawName)
      throws IOException {

    ElementState state;

    // Works much like content() with additions for closing an element. Note the different checks
    // for the closed element's state and the parent element's state.
    _printer.unindent();
    state = getElementState();

    if (state.empty) {
    	//ZOHO FIX: Closing br tag </br> inserts a new line break in browsers so self-closing it here
    	if("br".equalsIgnoreCase(rawName) && !requiresClosingTag(rawName)) {
    		_printer.printText(" />");
    	} else {
    		_printer.printText('>');
    	}
    }
    // This element is not empty and that last content was another element, so print a line break
    // before that last element and this element's closing tag. [keith] Provided this is not an
    // anchor. HTML: some elements do not print closing tag (e.g. LI)
    //ZOHO FIX: To add closing tag for <li> https://intranet.wiki.zoho.com/security/AntiSamyIssues.html#4._By_Gowri_Pradeep_%28Zoho_Sites%29
  	if (rawName == null || rawName.equalsIgnoreCase("li") || requiresClosingTag(rawName) || !HTMLdtd.isOnlyOpening(rawName)) {
      if (_indenting && !state.preserveSpace && state.afterElement) _printer.breakLine();
      // Must leave CData section first (Illegal in HTML, but still)
      if (state.inCData) _printer.printText("]]>");
      _printer.printText("</");
      _printer.printText(state.rawName);
      _printer.printText('>');
    }

    // Leave the element state and update that of the parent (if we're not root) to not empty and
    // after element.
    state = leaveElementState();
    // Temporary hack to prevent line breaks inside A/TD
    if (rawName == null || (!rawName.equalsIgnoreCase("A") && !rawName.equalsIgnoreCase("TD")))
      state.afterElement = true;
    state.empty = false;
    if (isDocumentState()) _printer.flush();
  }

  /**
   * ZOHO Method: to check if the tag requires a closing tag
   * @param tagName
   * @return
   */
  private boolean requiresClosingTag(String tagName) {
	return requireClosingTags.matches(tagName);
  }

  /*
   * The override is to use printEscaped() which already escapes entity references
   * and writes them in the final serialized string. As escapeURI() is called like
   * "printer.printText(escapeURI(value))", if the URI is returned here it would
   * be double-printed and that is why the return value is an empty string.
   */
  @Override
  protected String escapeURI(String uri) {
    try {
      printEscaped(uri);
    } catch (IOException e) {
      logger.error("URI escaping failed for value: " + uri);
    }
    return "";
  }
}
