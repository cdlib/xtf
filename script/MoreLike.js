//////////////////////////////////////////////////////////////////////////////
//
// MoreLike: handles sending off asynchronous requests that fetch "more
//           documents like this".
//
//////////////////////////////////////////////////////////////////////////////

// If the form with additional MoreLike parameters is present, add a parameter
// to the URL.
//
addTextParam = function( ark, name, url )
{
  var textElement = document.getElementById( ark + "-" + name );
  if( textElement == null )
      return url;
  else
      return url + "&" + name + "=" + textElement.value;
}

addFieldsParam = function( ark, url )
{
  var fields = "";
  for( var i = 1; ; i++ ) {
      var element = document.getElementById( ark + "-field-" + i );
      if( element == null )
          break;
      if( element.value == "0" || element.value == "0.0" )
          continue;
      if( i > 1 )
          fields += ",";
      fields += element.name;
      if( element.value != "1" && element.value != "1.0" )
          fields += "^" + element.value;
  }
  if( i == 1 )
      return url;
  else
      return url + "&fieldsToScan=" + fields;   
}
    
// Asynchronously fetch more docs "like this".
moreLike = function( ark, url )
{
  url = addTextParam( ark, "minWordLen", url );
  url = addTextParam( ark, "maxWordLen", url );
  url = addTextParam( ark, "minDocFreq", url );
  url = addTextParam( ark, "maxDocFreq", url );
  url = addTextParam( ark, "minTermFreq", url );
  url = addTextParam( ark, "termBoost", url );
  url = addTextParam( ark, "maxQueryTerms", url );
  url = addFieldsParam( ark, url );

  var element = document.getElementById( ark + "-moreLike" );
  //element.innerHTML = url; return;
  element.innerHTML = "Fetching...";
  var loader = new net.AsyncLoader( url, 
      function() { element.innerHTML = this.req.responseText; },
      function() { element.innerHTML = "Error."; } );
}