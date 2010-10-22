// XTF-specific customization of OpenLibrary Book Reader

// Set some config variables -- $$$ NB: Config object format has not been finalized
var brConfig = {};
brConfig["mode"] = 2;

br = new BookReader();
br.mode = 2;

//   We have three numbering systems to deal with here:
//   1. "Leaf": Numbered sequentially starting at 1. This is a page from the original scan. 
//              Most of these are to be shown but a few (e.g. color cards) should be suppressed
//              for pleasant viewing. Hence...
//              
//   2. "Index": Also sequential starting at 1, but representing only the viewable pages. So the
//               number of index values is less than or equal to the number of leaves.
//               Example: if pages 3-9 are not viewable, the mapping from Index to Leaf would be:
//               
//                  index 1 : leaf 1
//                  index 2 : leaf 2
//                  index 3 : leaf 10
//                  index 4 : leaf 11
//                  etc.
//               
//   3. "Page": This is the human-readable, logical page number. So for instance, the cover page
//              doesn't count, and embedded color plates are often not numbered. The number of
//              page numbers is less than or equal to the number of indexes. Continuing our
//              example from above, let's say that "Page 1" of the book is leaf 10. The
//              mapping from page to index to leaf is:
//              
//                 page 1 : index 3 : leaf 10
//                 page 2 : index 4 : leaf 11
//                 etc.

br.pageNumToPage = {};
br.indexToPage = {};
br.leafToPage = {};

br.Page = function(w, h, imgFile, leafNum, indexNum, pageNum) {
   this.w = w;
   this.h = h;
   this.imgFile = imgFile;
   this.leafNum = leafNum;
   this.indexNum = indexNum;
   this.pageNum = pageNum;
   
   br.leafToPage[leafNum] = this;
   br.indexToPage[indexNum] = this;
   br.pageNumToPage[pageNum] = this;
};

br.getPageWidth = function(index) {
   return (index in this.indexToPage) ? this.indexToPage[index].w : NaN;
}

br.getPageHeight = function(index) {
   return (index in this.indexToPage) ? this.indexToPage[index].h : NaN;
}

// Use the Djatoka image server to convert and scale the JP2 page files.
br.getPageURI = function(index, reduce, rotate) {
   var page = this.indexToPage[index];
   if (page === undefined)
      return undefined;
   return this.xtfDocDir  + page.imgFile;
}

br.getPageNum = function(index) {
   if (!(index in this.indexToPage))
      return null;
   var num = this.indexToPage[index].pageNum;
   return num ? num : "n"+index;
}

br.leafNumToIndex = function(leafNum) {
   var page = this.leafToPage[leafNum];
   if (page == undefined)
      return NaN;
   return page.indexNum;
}

br.getPageSide = function(index) {
   //assume the book starts with a cover (right-hand leaf)
   //we should really get handside from scandata.xml
   
   
   // $$$ we should get this from scandata instead of assuming the accessible
   //     leafs are contiguous
   if ('rl' != this.pageProgression) {
      // If pageProgression is not set RTL we assume it is LTR
      if (0 == (Math.abs(index) % 2)) {
         // Even-numbered page
         return 'R';
      } else {
         // Odd-numbered page
         return 'L';
      }
   } else {
      // RTL
      if (0 == (Math.abs(index) % 2)) {
         return 'L';
      } else {
         return 'R';
      }
   }
}

// This function returns the left and right indices for the user-visible
// spread that contains the given index.  The return values may be
// null if there is no facing page or the index is invalid.
br.getSpreadIndices = function(pindex) {
   // $$$ we could make a separate function for the RTL case and
   //      only bind it if necessary instead of always checking
   // $$$ we currently assume there are no gaps
   
   var spreadIndices = [null, null]; 
   if ('rl' == this.pageProgression) {
      // Right to Left
      if (this.getPageSide(pindex) == 'R') {
         spreadIndices[1] = pindex;
         spreadIndices[0] = pindex + 1;
      } else {
         // Given index was LHS
         spreadIndices[0] = pindex;
         spreadIndices[1] = pindex - 1;
      }
   } else {
      // Left to right
      if (this.getPageSide(pindex) == 'L') {
         spreadIndices[0] = pindex;
         spreadIndices[1] = pindex + 1;
      } else {
         // Given index was RHS
         spreadIndices[1] = pindex;
         spreadIndices[0] = pindex - 1;
      }
   }

   //console.log("   index %d mapped to spread %d,%d", pindex, spreadIndices[0], spreadIndices[1]);
   
   return spreadIndices;
}

br.canRotatePage = function(index) { return false; } // We don't support rotation (yet anyway)

br.imagesBaseURL = "css/bookreader/images";

br.autofit = "height"; // for some reason BookReader doesn't set this.

