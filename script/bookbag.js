/*=================================================================================================
This script provides functionality to add and remove documents from a user's bookbag.

It assumes that all bookbag links have a common CSS selector, as well as an attribute that uniquely
identifies a document. It also assumes that the element displaying the number of items in a bookbag
has a unique CSS selector. These values are controlled by variables:

bookbag - the CSS selector for all bookbag links.
identifier - an attribute of the bookbag link that uniquely identifies a document.
bagCount - the element which displays the number of items in the bookbag.
================================================================================================ */

$(document).ready(function () {
    // Sets global variables
    var bookbag = '.bookbag';
    var bagCount = '#bagCount';
    var identifier = 'data-identifier';
    
    // Removes link and changes text displayed if cookies are not enabled
    if (! navigator.cookieEnabled) {
        $(bookbag).text('Cookies not enabled');
    }
    
    // Main function to add and delete documents from bookbag
    $(bookbag).click(function (e) {
        var a = $(this);
        if (a.text() === 'Add') {
            // Add document to bookbag
            console.log('Adding component ' + $(a).attr(identifier))
            a.text('Adding...');
            $.ajax('?smode=addToBag;identifier=' + $(a).attr(identifier)).success(function () {
                // If add is successful, increase bookbag item count and change text
                var count = $(bagCount).text();
                $(bagCount).text(++ count);
                a.replaceWith('<span>Added</span>');
            }).fail(function () {
                // If add fails, change text and remove link
                a.replaceWith('<span>Failed to add</span>');
            });
        } else if (a.text() === 'Delete') {
            // Remove document from bookbag
            console.log('Deleting component ' + $(a).attr(identifier))
            a.text('Deleting...');
            $.ajax('?smode=removeFromBag;identifier=' + $(a).attr(identifier)).success(function () {
                //If delete is successful, decrease bookbag item count and hide document
                var count = $(bagCount).text();
                $(bagCount).text(-- count);
                a.closest('.docHit').hide();
            }).fail(function () {
                // If delete fails, change text and remove link
                a.replaceWith('<span>Failed to delete</span>');
            })
        }
        e.preventDefault();
    });
});