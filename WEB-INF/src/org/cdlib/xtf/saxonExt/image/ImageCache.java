package org.cdlib.xtf.saxonExt.image;

/**
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
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;

import javax.imageio.ImageIO;

import net.sf.saxon.trans.DynamicError;

import org.cdlib.xtf.cache.GeneratingCache;

/**
 * Maintain a cache of palette-mapped images used for image highlighting.
 * 
 * @author Martin Haye
 */
class ImageCache extends GeneratingCache<String, BufferedImage>
{
  private int outColorBase;
  
  /** Construct the cache */
  ImageCache(int outColorBase) {
    super(20, 300); // Keep up to 20 images, for 5 minutes max.
    this.outColorBase = outColorBase;
  }
  
  /** Read in and map an image */
  @Override
  protected BufferedImage generate(String filename) throws Exception
  {
    // Interesting workaround: using BufferedImage normally results in a Window
    // being created. However, since we're running in a servlet container, this
    // isn't generally desirable (and often isn't possible.) So we let AWT know
    // that it's running in "headless" mode, and this prevents the window from
    // being created.
    //
    System.setProperty("java.awt.headless", "true");
    
    // Okay, load the image.
    BufferedImage bi = ImageIO.read(new File(filename));
    bi = remapPalette(bi);
    return bi;
  }

  /**
   * Remap the colors in the given image, creating space for highlight
   * versions of the colors.
   * 
   * @param inImg    The image to remap
   * @return      A new image with reduced and normalized palette
   */
  private BufferedImage remapPalette(BufferedImage inImg)
    throws DynamicError
  {
    // Make sure we know how to deal with this image.
    if (!(inImg.getColorModel() instanceof IndexColorModel))
      throw new RuntimeException("image.output can only handle index color (palette) images");
    IndexColorModel inCm = (IndexColorModel) inImg.getColorModel();
    
    // First, get the input palette.
    int nInColors = inCm.getMapSize();
    byte[][] inColors = new byte[3][nInColors];
    inCm.getReds  (inColors[0]);
    inCm.getGreens(inColors[1]);
    inCm.getBlues (inColors[2]);
    
    // Create the output palette, which is always fixed (for the moment at least).
    int nOutColors = outColorBase*4;
    byte[][] outColors = new byte[3][nOutColors];
    for (int i=0; i<outColorBase; i++) 
    {
      // First comes the base color
      byte greyVal = (byte) (i * 255 / (outColorBase-1));
      outColors[0][i + outColorBase*0] = greyVal;   // red
      outColors[1][i + outColorBase*0] = greyVal;   // green
      outColors[2][i + outColorBase*0] = greyVal;   // blue
      
      // Make a set: white->yellow, black->black
      outColors[0][i + outColorBase*1] = greyVal;   // red
      outColors[1][i + outColorBase*1] = greyVal;   // green
      outColors[2][i + outColorBase*1] = 0;         // blue
      
      // Make a set: white->white, black->red
      byte redMapped = (byte) Math.max(192, ((int)greyVal) & 0xff); 
      outColors[0][i + outColorBase*2] = redMapped; // red
      outColors[1][i + outColorBase*2] = greyVal;   // green
      outColors[2][i + outColorBase*2] = greyVal;   // blue
      
      // And finally, a combination of the two
      outColors[0][i + outColorBase*3] = redMapped; // red
      outColors[1][i + outColorBase*3] = greyVal;   // green
      outColors[2][i + outColorBase*3] = 0;         // blue
    }
    
    // Form the mapping from the input palette to the output
    byte[] mapping = new byte[nInColors];
    for (int i=0; i<nInColors; i++) {
      int sum = (((int)inColors[0][i]) & 0xff) +
                (((int)inColors[1][i]) & 0xff) +
                (((int)inColors[2][i]) & 0xff);
      mapping[i] = (byte) (sum * (outColorBase-1) / (255*3));
    }
    
    // Okay, grab the input pixels
    WritableRaster inRast = inImg.getRaster();
    int w = inRast.getWidth();
    int h = inRast.getHeight();
    if (inRast.getTransferType() != DataBuffer.TYPE_BYTE)
      throw new RuntimeException("Unrecognized transfer type");
    if (inRast.getNumDataElements() != 1)
      throw new RuntimeException("How could index color have more than one data element?");
    byte[] inPixels = new byte[w*h];
    inRast.getDataElements(0, 0, w, h, inPixels);
    
    // Make the output bitmap
    IndexColorModel outCm = new IndexColorModel(8, nOutColors, outColors[0], outColors[1], outColors[2]);
    BufferedImage outImg = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, outCm);
    WritableRaster outRast = outImg.getRaster();
    if (outRast.getTransferType() != DataBuffer.TYPE_BYTE)
      throw new RuntimeException("Unrecognized transfer type");
    if (outRast.getNumDataElements() != 1)
      throw new RuntimeException("How could index color have more than one data element?");
    byte[] outPixels = new byte[w*h];
    
    // Map all the input pixels to their new values.
    for (int i=0; i<w*h; i++)
      outPixels[i] = mapping[inPixels[i]];
    outRast.setDataElements(0, 0, w, h, outPixels);
    
    // All done!
    return outImg;
  }
}
