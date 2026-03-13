<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<xsl:for-each select="Server/Service/Connector">
			<xsl:if test="not(@server)">
				<xsl:value-of select="@port"/>
				<xsl:text> port configuration - please add attribute as 'server="ZGS"'. </xsl:text>
			</xsl:if>
			<xsl:if test="@server!='ZGS'">
				<xsl:value-of select="@port"/>
				<xsl:text> port configuration - please check server attribute value. It should be 'server="ZGS"'. </xsl:text>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
