//////////////////////////////////////////////////////////////////////////////
//
// AsyncLoader: Utility class that handles asynchronous data fetches and
//              notification.
//
//////////////////////////////////////////////////////////////////////////////

// Namespacing
var net = new Object();

// Constructor
net.AsyncLoader = function( url, onload, onerror, method, params, contentType )
{
  this.req     = null;
  this.onload  = onload;
  this.onerror = onerror ? onerror : this.defaultError;
  this.loadXMLDoc( url, method, params, contentType );
}

// Constants
net.READY_STATE_UNINITIALIZED = 0;
net.READY_STATE_LOADING       = 1;
net.READY_STATE_LOADED        = 2;
net.READY_STATE_INTERACTIVE   = 3;
net.READY_STATE_COMPLETE      = 4;

// Variables and methods
net.AsyncLoader.prototype = 
{
  // Does the main work of firing up the request
  loadXMLDoc : function( url, method, params, contentType )
  {
    if( !method )
      method = "GET";
    if( !contentType && method=="POST" )
      contentType = 'application/x-www-form-urlencoded';

    if( window.XMLHttpRequest )
      this.req = new XMLHttpRequest();
    else if( window.ActiveXObject )
      this.req = new ActiveXObject("Microsoft.XMLHTTP");

    if( this.req ) {
      try 
      {
        var loader = this;
        this.req.onreadystatechange = function() {
          loader.onReadyState.call( loader );
        }
        this.req.open( method, url, true );
        if( contentType )
          this.req.setRequestHeader( 'Content-Type', contentType );
        this.req.send( params );
      } catch( err ) {
        this.onerror.call( this );
      }
    }
  },

  // Called at various stages during and at completion of the async load
  onReadyState : function()
  {
    var req = this.req;
    if( req.readyState == net.READY_STATE_COMPLETE ) {
      var httpStatus = req.status;
      if( httpStatus==200 || httpStatus==0 )
        this.onload.call(this);
      else
        this.onerror.call(this);
    }
  },

  // If the client provided no error function, this is called.
  defaultError : function() {
    alert( "error fetching data!"
         + "\n\nreadyState:"+this.req.readyState
         + "\nstatus: "+this.req.status
         + "\nheaders: "+this.req.getAllResponseHeaders() );
  },

  // Check if the load is still in progress.
  isLoading : function() {
    return this.req.readyState != net.READY_STATE_COMPLETE;
  }
};
