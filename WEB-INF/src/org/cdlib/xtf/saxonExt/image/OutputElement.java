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

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.cdlib.xtf.servletBase.TextServlet;
import org.cdlib.xtf.saxonExt.InstructionWithContent;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.TailCall;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.style.ExtensionInstruction;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

/**
 * Implements a Saxon instruction that reads an image from the filesystem,
 * optionally modifies it in various ways, and outputs it directly via the
 * current HttpServletResponse.
 *
 * @author Martin Haye
 */
public class OutputElement extends ExtensionInstruction 
{
  HashMap<String, Expression> attribs = new HashMap<String, Expression>();
  boolean flipY = false;
  
  private final static int        outColorBase = 32;
  private final static ImageCache imageCache = new ImageCache(outColorBase);

  public void prepareAttributes()
    throws XPathException 
  {
    // Check the attributes.
    AttributeCollection inAtts = getAttributeList();
    for (int i=0; i<inAtts.getLength(); i++) {
      String attName = inAtts.getLocalName(i);
      String attVal = inAtts.getValue(i);
      if (attName.matches("^(src|xBias|xScale|yBias|yScale)$"))
        attribs.put(attName, makeAttributeValueTemplate(attVal));
      else if (attName.equals("flipY")) {
        if (attVal.matches("^(1|true|yes)$"))
          flipY = true;
        else if (attVal.matches("^(0|false|no)$"))
          flipY = false;
        else
          this.compileError("'flipy' attribute must be 'yes' or 'no'");
      }
      else
        this.compileError("Unrecogized attribute '" + attName + "'for image:output element");
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
    return new OutputInstruction(attribs, flipY, content);
  }

  private static class OutputInstruction extends InstructionWithContent 
  {
    private boolean    flipY;
    private float      xBias;
    private float      xScale;
    private float      yBias;
    private float      yScale;
    private int        origHeight;
    private int        cropOffX;
    private int        cropOffY;

    public OutputInstruction(HashMap<String, Expression> attribs, boolean flipY, Expression content) 
    {
      super("image:output", attribs, content);
      this.flipY = flipY;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException 
    {
      cropOffX = cropOffY = 0;
      
      try 
      {
        // Interesting workaround: using BufferedImage normally results in a Window
        // being created. However, since we're running in a servlet container, this
        // isn't generally desirable (and often isn't possible.) So we let AWT know
        // that it's running in "headless" mode, and this prevents the window from
        // being created.
        //
        System.setProperty("java.awt.headless", "true");
        
        // Get the bias and scale factors, if any.
        xBias  = getFloatAttrib(context, "xBias",  0);
        xScale = getFloatAttrib(context, "xScale", 1);
        yBias  = getFloatAttrib(context, "yBias",  0);
        yScale = getFloatAttrib(context, "yScale", 1);
        
        // First, load the source image. The ImageCache will automatically take
        // care of mapping the palette for us.
        //
        String src = attribs.get("src").evaluateAsString(context);
        String srcPath = TextServlet.getCurServlet().getRealPath(src);
        BufferedImage bi;
        try {
          bi = imageCache.find(srcPath);
        }
        catch (Exception e) { throw new RuntimeException(e); }
        
        // Make a copy of the image so we don't mess up the original.
        bi = new BufferedImage(bi.getColorModel(), (WritableRaster) bi.getData(), false, null);
            
        // Record the original height (needed for flipY mode)
        origHeight = bi.getHeight();
        
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
            Rect rect = parseRect(context, node, bi.getWidth(), bi.getHeight());
            if (!rect.isEmpty())
            {
              // Highlight the area
              if (node.getLocalPart().equals("yellowBackground"))
                makeBackgroundYellow(bi, rect);
              else if (node.getLocalPart().equals("redForeground"))
                makeForegroundRed(bi, rect);
              else {
                bi = bi.getSubimage(rect.left, rect.top, rect.width(), rect.height());
                cropOffX += rect.left;
                cropOffY += rect.top;
              }
            }
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
     * Get an attribute value and convert to floating point. If not present,
     * the default value is used instead.
     * @throws XPathException 
     */
    private float getFloatAttrib(XPathContext context, String attName, float defaultVal) 
      throws XPathException
    {
      String strVal = attribs.get(attName).evaluateAsString(context);
      if (strVal == null)
        return defaultVal;
      try {
        return Float.parseFloat(strVal);
      }
      catch (NumberFormatException e) {
        return defaultVal;
      }
    }

    /**
     * Parse the "left", "top", "right", and "bottom" attributes from a 
     * "highlight" element.
     * 
     * @param node  The element containing the attributes
     * @param imgHeight The height to use when flipping
     * @return A rectangle containing the coordinates (flipped if necessary)
     * @throws DynamicError 
     */
    private Rect parseRect(XPathContext context, NodeInfo node, int imgWidth, int imgHeight) 
      throws DynamicError
    {
      AxisIterator atts = node.iterateAxis(Axis.ATTRIBUTE);
      Item att;
      int left = 0, top = 0, right = 0, bottom = 0;
      boolean leftFound = false, rightFound = false, topFound = false, bottomFound = false;
      while ((att = atts.next()) != null)
      {
        String name = ((NodeInfo)att).getLocalPart();
        String value = ((NodeInfo)att).getStringValue();
        if ("left".equals(name)) {
          left = Integer.parseInt(value); leftFound = true;
        }
        else if ("top".equals(name)) {
          top = Integer.parseInt(value); topFound = true;
        }
        else if ("right".equals(name)) { 
          right = Integer.parseInt(value); rightFound = true;
        }
        else if ("bottom".equals(name)) {
          bottom = Integer.parseInt(value); bottomFound = true;
        }
        else
          dynamicError("Unknown attribute '" + name + "' to '" + node.getLocalPart() + "' element", "XTFimgHAU", context);
      }
      
      if (!(leftFound && rightFound && topFound && bottomFound))
        dynamicError("'" + node.getLocalPart() + "' element must specify 'left', 'top', 'right', and 'bottom' attributes", "XTFimgHAltrb", context);
      
      // Apply bias and scale factors.
      left    = (int) ((left    + xBias) * xScale);
      top     = (int) ((top     + yBias) * yScale);
      right   = (int) ((right   + xBias) * xScale);
      bottom  = (int) ((bottom  + yBias) * yScale);
      
      // Flip the Y values if requested
      if (flipY) {
        top    = origHeight - top;
        bottom = origHeight - bottom;
      }
      
      // If cropping has been done, adjust the coordinates.
      left   -= cropOffX;
      top    -= cropOffY;
      right  -= cropOffX;
      bottom -= cropOffY;
      
      // Clamp the values to be completely within the image.
      left   = Math.max(Math.min(left,   imgWidth),  0);
      top    = Math.max(Math.min(top,    imgHeight), 0);
      right  = Math.max(Math.min(right,  imgWidth), 0);
      bottom = Math.max(Math.min(bottom, imgHeight),  0);
      
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
      byte[] data = new byte[w*h];
      bi.getRaster().getDataElements(rect.left, rect.top, rect.width(), rect.height(), data);
      for (int i=0; i<w*h; i++)
        data[i] |= (outColorBase * 1);
      bi.getRaster().setDataElements(rect.left, rect.top, rect.width(), rect.height(), data);
    }

    /**
     * Change black foreground to red in the given area of an image
     */
    private void makeForegroundRed(BufferedImage bi, Rect rect)
    {
      int w = rect.width();
      int h = rect.height();
      byte[] data = new byte[w*h];
      bi.getRaster().getDataElements(rect.left, rect.top, rect.width(), rect.height(), data);
      for (int i=0; i<w*h; i++)
        data[i] |= (outColorBase * 2);
      bi.getRaster().setDataElements(rect.left, rect.top, rect.width(), rect.height(), data);
    }
  }

  /** A rectangle on the image */
  private static class Rect
  {
    public Rect(int l, int t, int r, int b) {
      left = l; top = t; right = r; bottom = b;
    }
    
    public boolean isEmpty() {
      return (width() <= 0 || height() <= 0);
    }

    public int left;
    public int top;
    public int right;
    public int bottom;
    
    public int width()  { return right - left; }
    public int height() { return bottom - top; }
  }

} // class OutputElement
