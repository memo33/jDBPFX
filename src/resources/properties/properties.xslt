<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/">
		<html>
			<head>
				<title>SimCity 4 Exemplar Properties</title>
			</head>
			<body bgcolor="white">
				<div align="center"><h2>SimCity 4 Exemplar Properties</h2>
				<table border="0" cellpadding="0" cellspacing="0">
					<xsl:apply-templates select="//group"/>
				</table>
				</div>
			</body>
		</html>
	</xsl:template>
	
	<xsl:template match="group">
					<tr>
						<td width="830" bgcolor="orange" colspan="4" style="font-style: bold;"><xsl:value-of select="@name"/></td>
					</tr>
					<tr>
						<th width="100"  bgcolor="coral">Number</th>
						<th width="80"  bgcolor="green">Type</th>
						<th width="250"  bgcolor="blue">Name</th>
						<th width="400"  bgcolor="yellow">Description</th>
					</tr>
					<xsl:apply-templates select="property" />
	</xsl:template>

	<xsl:template match="property">
					<tr>
						<td width="100" bgcolor="lightcoral"><xsl:value-of select="@num"/></td>
						<td width="80" bgcolor="lightgreen"><xsl:value-of select="@type"/></td>
						<td width="250" bgcolor="lightblue"><xsl:value-of select="@name"/></td>
						<td width="400" bgcolor="lightyellow"><xsl:value-of select="@desc"/></td>
					</tr>
	</xsl:template>
</xsl:stylesheet>

  