package org.cdlib.xtf.lazyTree;
import net.sf.saxon.om.*;
import org.xml.sax.Attributes;


/**
* Saxon: AttributeCollection is an implementation of the SAX2 interface Attributes
* that also provides the ability to manipulate namespaces and to convert attributes
* into Nodes.
*
* It is extremely similar (both in interface and in implementation) to the SAX2 Attributes
* class, but was defined before SAX2 was available.
*/

public final class AttributeCollection implements Attributes
{

    // we use a single array for economy. The elements of this array are arranged
    // in groups of three, being respectively the nameCode, the
    // type, and the value

    private NamePool namePool;
    private Object[] list = null;
    private int used = 0;

    private static int RECSIZE = 3;
    private static int NAMECODE = 0;
    private static int TYPE = 1;
    private static int VALUE = 2;

    /**
    * Create an empty attribute list.
    */

    public AttributeCollection (NamePool pool) {
        namePool = pool;
        list = null;
        used = 0;
     }


    /**
    * Add an attribute to an attribute list.
    * @param nameCode Integer representing the attribute name.
    * @param type The attribute type ("NMTOKEN" for an enumeration).
    * @param value The attribute value (must not be null).
    */

    public void addAttribute (int nameCode, String type, String value)
    {
       if (list==null) {
            list = new Object[5*RECSIZE];
            used = 0;
        }
        if (list.length == used) {
            int newsize = (used==0 ? 5*RECSIZE : used*2);
            Object[] newlist = new Object[newsize];
            System.arraycopy(list, 0, newlist, 0, used);
            list = newlist;
        }
        list[used++] = new Integer(nameCode);
        list[used++] = type;
        list[used++] = value;
    }

    /**
    * Clear the attribute list.
    */

    public void clear () {
        used = 0;
    }

    /**
    * Compact the attribute list to avoid wasting memory
    */

    public void compact() {
        if (used==0) {
            list = null;
        } else if (list.length > used) {
            Object[] newlist = new Object[used];
            System.arraycopy(list, 0, newlist, 0, used);
            list = newlist;
        }
    }


    //////////////////////////////////////////////////////////////////////
    // Implementation of org.xml.sax.Attributes
    //////////////////////////////////////////////////////////////////////


    /**
    * Return the number of attributes in the list.
    * @return The number of attributes in the list.
    */

    public int getLength ()
    {
        return (list==null ? 0 : used / RECSIZE );
    }

    /**
    * Get the namecode of an attribute (by position).
    *
    * @param index The position of the attribute in the list.
    * @return The display name of the attribute as a string, or null if there
    *         is no attribute at that position.
    */

    public int getNameCode (int index)
    {
        int offset = index*RECSIZE;
        if (list==null) return -1;
        if (offset >= used) return -1;

        return ((Integer)list[offset+NAMECODE]).intValue();
    }

    /**
    * Get the display name of an attribute (by position).
    *
    * @param index The position of the attribute in the list.
    * @return The display name of the attribute as a string, or null if there
    *         is no attribute at that position.
    */

    public String getQName (int index)
    {
        int offset = index*RECSIZE;
        if (list==null) return null;
        if (offset >= used) return null;
        return namePool.getDisplayName(getNameCode(index));
    }

    /**
    * Get the local name of an attribute (by position).
    *
    * @param index The position of the attribute in the list.
    * @return The local name of the attribute as a string, or null if there
    *         is no attribute at that position.
    */

    public String getLocalName (int index)
    {
        if (list==null) return null;
        if (index*RECSIZE >= used) return null;
        return namePool.getLocalName(getNameCode(index));
    }

    /**
    * Get the namespace URI of an attribute (by position).
    *
    * @param index The position of the attribute in the list.
    * @return The local name of the attribute as a string, or null if there
    *         is no attribute at that position.
    */

    public String getURI (int index)
    {
        if (list==null) return null;
        if (index*RECSIZE >= used) return null;
        return namePool.getURI(getNameCode(index));
    }



    /**
    * Get the type of an attribute (by position).
    * @param index The position of the attribute in the list.
    * @return The attribute type as a string ("NMTOKEN" for an
    *         enumeration, and "CDATA" if no declaration was
    *         read), or null if there is no attribute at
    *         that position.
    */

    public String getType (int index)
    {
        int offset = index*RECSIZE;
        if (list==null) return null;
        if (offset >= used) return null;
        return (String)list[offset+TYPE];
    }

    /**
    * Get the type of an attribute (by name).
    *
    * @param uri The namespace uri of the attribute.
    * @param localname The local name of the attribute.
    * @return The index position of the attribute
    */

    public String getType (String uri, String localname)
    {
        int offset = findByName(uri, localname);
        return ( offset<0 ? null : (String)list[offset+TYPE]);
    }

    /**
    * Get the value of an attribute (by position).
    *
    * @param index The position of the attribute in the list.
    * @return The attribute value as a string, or null if
    *         there is no attribute at that position.
    */

    public String getValue (int index) {
        int offset = index*RECSIZE;
        if (list==null) return null;
        if (offset >= used) return null;
        return (String)list[offset+VALUE];
    }

    /**
    * Get the value of an attribute (by name).
    *
    * @param uri The namespace uri of the attribute.
    * @param localname The local name of the attribute.
    * @return The index position of the attribute
    */

    public String getValue (String uri, String localname)
    {
        int offset = findByName(uri, localname);
        return ( offset<0 ? null : (String)list[offset+VALUE]);
    }

    /**
    * Get the attribute value using its fingerprint
    */

    public String getValueByFingerprint(int fingerprint) {
        int offset = findByFingerprint(fingerprint);
        return ( offset<0 ? null : (String)list[offset+VALUE]);
    }

    /**
    * Get the index of an attribute (by name).
    *
    * @param name The display name of the attribute.
    * @return The index position of the attribute
    */

    public int getIndex (String name)
    {
        int offset = findByDisplayName(name);
        return ( offset<0 ? -1 : offset / RECSIZE);
    }

    /**
    * Get the index of an attribute (by name).
    *
    * @param uri The namespace uri of the attribute.
    * @param localname The local name of the attribute.
    * @return The index position of the attribute
    */

    public int getIndex (String uri, String localname)
    {
        int offset = findByName(uri, localname);
        return ( offset<0 ? -1 : offset / RECSIZE);
    }

    /**
    * Get the index, given the fingerprint
    */

    public int getIndexByFingerprint(int fingerprint) {
        int offset = findByFingerprint(fingerprint);
        return ( offset<0 ? -1 : offset / RECSIZE);
    }

    /**
    * Get the type of an attribute (by name).
    *
    * @param name The display name of the attribute.
    * @return The attribute type as a string ("NMTOKEN" for an
    *         enumeration, and "CDATA" if no declaration was
    *         read).
    */

    public String getType (String name)
    {
        int offset = findByDisplayName(name);
        return ( offset<0 ? null : (String)list[offset+TYPE]);
    }


    /**
    * Get the value of an attribute (by name).
    *
    * @param name The attribute name.
    */

    public String getValue (String name)
    {
        int offset = findByDisplayName(name);
        return ( offset<0 ? null : (String)list[offset+VALUE]);
    }

    //////////////////////////////////////////////////////////////////////
    // Additional methods for handling structured Names
    //////////////////////////////////////////////////////////////////////

    /**
    * Find an attribute by name
    * @return the offset of the attribute, or -1 if absent
    */

    private int findByName(String uri, String localName) {
        if (namePool==null) return -1;        // indicates an empty attribute set
        int f = namePool.getFingerprint(uri, localName);
        if (f==-1) return -1;
        return findByFingerprint(f);
    }

    /**
    * Find an attribute by fingerprint
    * @return the offset of the attribute, or -1 if absent
    */

    private int findByFingerprint(int fingerprint) {
        if (list==null) return -1;
        for (int i=0; i<used; i+=RECSIZE) {
            if (fingerprint==(((Integer)list[i+NAMECODE]).intValue()&0xfffff)) {
                return i;
            }
        }
        return -1;
    }

    /**
    * Find an attribute by display name
    * @return the offset of the attribute
    */

    private int findByDisplayName(String qname) {
        if (list==null) {
            return -1;
        }
        String[] parts;
        try {
            parts = Name.getQNameParts(qname);
        } catch (QNameException err) {
            return -1;
        }
        String prefix = parts[0];
        if (prefix.equals("")) {
            return findByName("", qname);
        } else {
            String localName = parts[1];
            for (int i=0; i<getLength(); i++) {
                String lname=namePool.getLocalName(getNameCode(i));
                String ppref=namePool.getPrefix(getNameCode(i));
                if (localName.equals(lname) && prefix.equals(ppref)) {
                    return i;
                }
            }
            return -1;
        }
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/ 
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License. 
//
// The Original Code is: most of this file. 
//
// The Initial Developer of the Original Code is
// Michael Kay of International Computers Limited (michael.h.kay@ntlworld.com).
//
// Portions created by Martin Haye are Copyright (C) Regents of the University 
// of California. All Rights Reserved. 
//
// Contributor(s): Martin Haye. 
//
