package org.cdlib.xtf.saxonExt.image;


/*
 * Copyright (c) 2007, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the University of California nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.cdlib.xtf.servletBase.TextServlet;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.Optimizer;
import net.sf.saxon.expr.PromotionOffer;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.Instruction;
import net.sf.saxon.instruct.TailCall;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;

/**
 * Implements a Saxon instruction that reads an image from the filesystem,
 * optionally modifies it in various ways, and outputs it directly via the
 * current HttpServletResponse.
 *
 * @author Martin Haye
 */
public class OutputElement extends ExtensionInstruction 
{
  Expression srcExp;
  boolean flipY = false;

  public void prepareAttributes()
    throws XPathException 
  {
    // Get mandatory 'src' attribute
    String srcAtt = getAttributeList().getValue("", "src");
    if (srcAtt == null) {
      reportAbsence("src");
      return;
    }
    srcExp = makeAttributeValueTemplate(srcAtt);
    
    // Get optional 'flipY' attribute
    String flipYStr = getAttributeList().getValue("", "flipY");
    if (flipYStr != null) {
      if ("no".equalsIgnoreCase(flipYStr))
        flipY = false;
      else if ("yes".equalsIgnoreCase(flipYStr))
        flipY = true;
      else
        this.compileError("'flipy' attribute must be 'yes' or 'no'");
    }
  } // prepareAttributes()

  /**
   * Determine whether this type of element is allowed to contain a template-body
   */
  public boolean mayContainSequenceConstructor() {
    return true;
  }

  
  public Expression compile(Executable exec)
    throws XPathException 
  {
    Expression content = compileSequenceConstructor(exec, iterateAxis(Axis.CHILD), true);
    return new OutputInstruction(srcExp, flipY, content);
  }

  private class OutputInstruction extends Instruction 
  {
    private Expression srcExp;
    private boolean    flipY;
    private Expression content;

    public OutputInstruction(Expression srcExp, boolean flipY, Expression content) 
    {
      this.srcExp = srcExp;
      this.flipY = flipY;
      this.content = content;
      adoptChildExpression(srcExp);
      adoptChildExpression(content);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */
    public Expression simplify(StaticContext env) throws XPathException {
        srcExp = srcExp.simplify(env);
        if (content != null) {
            content = content.simplify(env);
        }
        return this;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        srcExp = srcExp.typeCheck(env, contextItemType);
        adoptChildExpression(srcExp);
        if (content != null) {
            content = content.typeCheck(env, contextItemType);
            adoptChildExpression(content);
        }
        return this;
    }

   public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        srcExp = srcExp.optimize(opt, env, contextItemType);
        adoptChildExpression(srcExp);
        if (content != null) {
            content = content.optimize(opt, env, contextItemType);
            adoptChildExpression(content);
        }
        return this;
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_MESSAGE;
    }

    public ItemType getItemType(TypeHierarchy th) {
        return EmptySequenceTest.getInstance();
    }

    public int getCardinality() {
        return StaticProperty.EMPTY;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true.
     */
    public final boolean createsNewNodes() {
        return true;
    }
    
    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */
    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (srcExp != null) {
            srcExp = doPromotion(srcExp, offer);
        }
        if (content != null) {
            content = doPromotion(content, offer);
        }
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(2);
        if (srcExp != null) {
            list.add(srcExp);
        }
        if (content != null) {
            list.add(content);
        }
        return list.iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (srcExp == original) {
            srcExp = replacement;
            found = true;
        }
        if (content == original) {
            content = replacement;
            found = true;
        }
        return found;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException 
    {
      try 
      {
        // First, load the source image.
        String src = srcExp.evaluateAsString(context);
        String srcPath = TextServlet.getCurServlet().getRealPath(src);
        BufferedImage bi = ImageIO.read(new File(srcPath));

        Graphics g = bi.getGraphics();
        int imgWidth = bi.getWidth();
        int imgHeight = bi.getHeight();
        
        // Highlight specified areas, if any.
        if (content != null) 
        {
          SequenceIterator iter = content.iterate(context);
          Item item;
          while ((item = iter.next()) != null)
          {
            // The only sub-elements allowed are "highlight" elements
            if (!(item instanceof NodeInfo) || 
                ((NodeInfo)item).getNodeKind() != Type.ELEMENT ||
                !((NodeInfo)item).getLocalPart().matches("^(yellowBackground|redForeground|crop)$"))
            {
              dynamicError("image:output element may only contain 'yellowBackground', " +
                           "'redForeground', or 'crop' sub-elements", "XTFimgH", context);
            }
            
            // Parse the coordinate attributes
            NodeInfo node = (NodeInfo) item;
            Rect rect = parseRect(context, node, flipY, imgWidth, imgHeight);
              
            // Highlight the area
            if (node.getLocalPart().equals("yellowBackground"))
              makeBackgroundYellow(bi, rect);
            else if (node.getLocalPart().equals("redForeground"))
              makeForegroundRed(bi, rect);
            else
              bi = bi.getSubimage(rect.left, rect.top, rect.width(), rect.height());
          }
        }
        
        // Finally, output the result.
        HttpServletResponse res = TextServlet.getCurResponse();
        res.setContentType("image/png");
        ServletOutputStream out = res.getOutputStream();
        ImageIO.write(bi, "PNG", out);
      }
      catch (IOException e) {
        throw new DynamicError(e);
      }
 
      return null;
    }
    
    /**
     * Parse the "left", "top", "right", and "bottom" attributes from a 
     * "highlight" element.
     * 
     * @param node  The element containing the attributes
     * @param flipY Whether to flip vertical coordinates
     * @param imgHeight The height to use when flipping
     * @return A rectangle containing the coordinates (flipped if necessary)
     * @throws DynamicError 
     */
    private Rect parseRect(XPathContext context, NodeInfo node, 
                           boolean flipY, int imgWidth, int imgHeight) 
      throws DynamicError
    {
      AxisIterator atts = node.iterateAxis(Axis.ATTRIBUTE);
      Item att;
      int left = -1;
      int top = -1;
      int right = -1;
      int bottom = -1;
      while ((att = atts.next()) != null)
      {
        String name = ((NodeInfo)att).getLocalPart();
        String value = ((NodeInfo)att).getStringValue();
        if ("left".equals(name))
          left = Integer.parseInt(value);
        else if ("top".equals(name))
          top = Integer.parseInt(value);
        else if ("right".equals(name))
          right = Integer.parseInt(value);
        else if ("bottom".equals(name))
          bottom = Integer.parseInt(value);
        else
          dynamicError("Unknown attribute '" + name + "' to 'highlight' element", "XTFimgHAU", context);
      }
      
      if (left < 0 || top < 0 || right < 0 || bottom < 0)
        dynamicError("'highlight' element must specify 'left', 'top', 'right', and 'bottom' attributes", "XTFimgHAltrb", context);
      
      // Clamp the values to the valid range.
      left   = Math.max(Math.min(left,   imgWidth),  0);
      top    = Math.max(Math.min(top,    imgHeight), 0);
      right  = Math.max(Math.min(right,  imgWidth), 0);
      bottom = Math.max(Math.min(bottom, imgHeight),  0);
      
      // Flip the Y values if requested
      if (flipY) {
        top    = imgHeight - top;
        bottom = imgHeight - bottom;
      }
      
      // Reverse values that are backwards.
      int tmp;
      if (left > right) {
        tmp = left; left = right; right = tmp;
      }
      if (top > bottom) {
        tmp = top; top = bottom; bottom = tmp;
      }
      
      // All done.
      return new Rect(left, top, right, bottom);
    }
    
    /**
     * Change white background to yellow in the given area of an image
     */
    private void makeBackgroundYellow(BufferedImage bi, Rect rect)
    {
      int w = rect.width();
      int h = rect.height();
      int[] data = new int[w*h];
      bi.getRGB(rect.left, rect.top, rect.width(), rect.height(), data, 0, w);
      for (int i=0; i<w*h; i++) 
      {
        data[i] &= 0xffffff00;
      }
      bi.setRGB(rect.left, rect.top, rect.width(), rect.height(), data, 0, w);
    }

    /**
     * Change black foreground to red in the given area of an image
     */
    private void makeForegroundRed(BufferedImage bi, Rect rect)
    {
      int w = rect.width();
      int h = rect.height();
      int[] data = new int[w*h];
      bi.getRGB(rect.left, rect.top, rect.width(), rect.height(), data, 0, w);
      for (int i=0; i<w*h; i++)
      {
        int d = data[i];
        
        int r = (d >> 16) & 0xff;
        int g = (d >>  8) & 0xff;
        int b = (d >>  0) & 0xff;

        int f1 = g;
        int f2 = 255 - f1; 
          
        r = ((r * f1) + (192 * f2)) / 255;
        g = ((g * f1) + (  0 * f2)) / 255;
        b = ((b * f1) + (  0 * f2)) / 255;
        
        data[i] = (r<<16) | (g<<8) | b;
      }
      bi.setRGB(rect.left, rect.top, rect.width(), rect.height(), data, 0, w);
    }
    
    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     * @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "output");
    }
  }

  /** A rectangle on the image */
  private static class Rect
  {
    public Rect(int l, int t, int r, int b) {
      left = l; top = t; right = r; bottom = b;
    }
    
    public int left;
    public int top;
    public int right;
    public int bottom;
    
    public int width()  { return right - left; }
    public int height() { return bottom - top; }
  }

} // class OutputElement
