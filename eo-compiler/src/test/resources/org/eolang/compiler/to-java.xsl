<?xml version="1.0"?>
<!--
The MIT License (MIT)

Copyright (c) 2017-2019 Yegor Bugayenko

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"  xmlns:eo="https://www.eolang.org" xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xsl:variable name="EOL">
    <xsl:text>&#x0a;</xsl:text>
  </xsl:variable>
  <xsl:variable name="TAB">
    <xsl:text>  </xsl:text>
  </xsl:variable>
  <xsl:function name="eo:name" as="xs:string">
    <xsl:param name="n" as="xs:string"/>
    <xsl:sequence select="replace($n, '\+', '_')"/>
  </xsl:function>
  <xsl:template match="/program/objects/o[@name and not(@base)]">
    <xsl:variable name="attributes" select="./o[@name and not(@base) and not(./o)]"/>
    <xsl:variable name="methods" select="./o[not(@name and not(@base) and not(./o))]"/>
    <xsl:copy>
      <xsl:element name="java">
        <xsl:value-of select="$EOL"/>
        <xsl:text>public final class </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:if test="o[not(@name)]">
          <xsl:text> implements java.util.concurrent.Callable&lt;Object&gt;</xsl:text>
        </xsl:if>
        <xsl:text> {</xsl:text>
        <xsl:value-of select="$EOL"/>
        <xsl:for-each select="$attributes">
          <xsl:value-of select="$TAB"/>
          <xsl:text>private final Object </xsl:text>
          <xsl:value-of select="eo:name(@name)"/>
          <xsl:text>;</xsl:text>
          <xsl:value-of select="$EOL"/>
        </xsl:for-each>
        <xsl:value-of select="$TAB"/>
        <xsl:text>public </xsl:text>
        <xsl:value-of select="@name"/>
        <xsl:text>(</xsl:text>
        <xsl:for-each select="$attributes">
          <xsl:if test="position()!=1">
            <xsl:text>, </xsl:text>
          </xsl:if>
          <xsl:text>final Object </xsl:text>
          <xsl:value-of select="eo:name(@name)"/>
        </xsl:for-each>
        <xsl:text>) {</xsl:text>
        <xsl:value-of select="$EOL"/>
        <xsl:for-each select="$attributes">
          <xsl:value-of select="$TAB"/>
          <xsl:value-of select="$TAB"/>
          <xsl:text>this.</xsl:text>
          <xsl:value-of select="eo:name(@name)"/>
          <xsl:text> = </xsl:text>
          <xsl:value-of select="eo:name(@name)"/>
          <xsl:text>;</xsl:text>
          <xsl:value-of select="$EOL"/>
        </xsl:for-each>
        <xsl:value-of select="$TAB"/>
        <xsl:text>}</xsl:text>
        <xsl:value-of select="$EOL"/>
        <xsl:for-each select="$methods">
          <xsl:apply-templates select="." mode="method"/>
        </xsl:for-each>
        <xsl:text>}</xsl:text>
        <xsl:value-of select="$EOL"/>
      </xsl:element>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="o" mode="method">
    <xsl:value-of select="$TAB"/>
    <xsl:if test="not(@name)">
      <xsl:text>@Override</xsl:text>
      <xsl:value-of select="$EOL"/>
      <xsl:value-of select="$TAB"/>
      <xsl:text>public Object call() throws Exception</xsl:text>
      <xsl:value-of select="@name"/>
    </xsl:if>
    <xsl:if test="@name">
      <xsl:text>public Object </xsl:text>
      <xsl:value-of select="@name"/>
      <xsl:text>()</xsl:text>
    </xsl:if>
    <xsl:text> {</xsl:text>
    <xsl:value-of select="$EOL"/>
    <xsl:value-of select="$TAB"/>
    <xsl:value-of select="$TAB"/>
    <xsl:text>return </xsl:text>
    <xsl:apply-templates select=".">
      <xsl:with-param name="indent">
        <xsl:value-of select="$TAB"/>
        <xsl:value-of select="$TAB"/>
      </xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>;</xsl:text>
    <xsl:value-of select="$EOL"/>
    <xsl:value-of select="$TAB"/>
    <xsl:text>}</xsl:text>
    <xsl:value-of select="$EOL"/>
  </xsl:template>
  <xsl:template match="o[@base and @ref]">
    <xsl:text>this.</xsl:text>
    <xsl:value-of select="@base"/>
  </xsl:template>
  <xsl:template match="o[starts-with(@base, '.') and ./o]">
    <xsl:param name="indent"/>
    <xsl:apply-templates select="./o[1]">
      <xsl:with-param name="indent">
        <xsl:value-of select="$indent"/>
      </xsl:with-param>
    </xsl:apply-templates>
    <xsl:value-of select="@base"/>
    <xsl:text>(</xsl:text>
    <xsl:apply-templates select="./o[position() &gt; 1]">
      <xsl:with-param name="indent">
        <xsl:value-of select="$indent"/>
      </xsl:with-param>
    </xsl:apply-templates>
    <xsl:text>)</xsl:text>
  </xsl:template>
  <xsl:template match="o[@base and not(starts-with(@base, '.')) and not(@ref) and not(text())]">
    <xsl:param name="indent"/>
    <xsl:variable name="newindent">
      <xsl:value-of select="$indent"/>
      <xsl:value-of select="$TAB"/>
    </xsl:variable>
    <xsl:text>new </xsl:text>
    <xsl:value-of select="@base"/>
    <xsl:text>(</xsl:text>
    <xsl:if test="./o">
      <xsl:value-of select="$EOL"/>
      <xsl:value-of select="$newindent"/>
    </xsl:if>
    <xsl:for-each select="./o[not(@anonymous)]">
      <xsl:apply-templates select=".">
        <xsl:with-param name="indent">
          <xsl:value-of select="$newindent"/>
        </xsl:with-param>
      </xsl:apply-templates>
      <xsl:if test="position() != last()">
        <xsl:text>,</xsl:text>
        <xsl:value-of select="$EOL"/>
        <xsl:value-of select="$newindent"/>
      </xsl:if>
    </xsl:for-each>
    <xsl:if test="./o">
      <xsl:value-of select="$EOL"/>
      <xsl:value-of select="$indent"/>
    </xsl:if>
    <xsl:text>)</xsl:text>
  </xsl:template>
  <xsl:template match="o[text() and @base='org.eolang.float' or @base='org.eolang.integer' or @base='org.eolang.hex']">
    <xsl:value-of select="text()"/>
  </xsl:template>
  <xsl:template match="o[text() and @base='org.eolang.string']">
    <xsl:text>"</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>"</xsl:text>
  </xsl:template>
  <xsl:template match="o[text() and @base='org.eolang.char']">
    <xsl:text>'</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text>'</xsl:text>
  </xsl:template>
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
