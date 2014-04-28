/*===========================================================================================================
This script provides functionality to search and display similar documents via a link.

It assumes that all "similar items" links have a common CSS selector, as well as an attribute that uniquely 
identifies a document. These values are controlled by variables:

moreLike - the CSS selector for all "similar items" links.
identifier - an attribute of the bookbag link that uniquely identifies a document.
========================================================================================================== */

$(document).ready(function () {
    // Sets global variables
    var moreLike = '.moreLike';
    var identifier = 'data-identifier';
    
    $(moreLike).click(function (e) {
        var a = $(this);
        a.text('Fetching...');
        $.ajax('?smode=moreLike;docsPerPage=5;identifier=' + $(a).attr(identifier)).success(function (data) {
            // If results were retrieved, display them
            a.replaceWith(data);
        }).fail(function () {
            // If no results were retrieved, change text
            a.replaceWith('<span>Failed!</span>');
        });
        e.preventDefault();
    });
});