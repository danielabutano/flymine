package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.intermine.objectstore.ObjectStoreException;
import org.intermine.xml.full.Item;
import org.intermine.xml.full.Attribute;
import org.intermine.xml.full.Reference;
import org.intermine.xml.full.ReferenceList;
import org.intermine.xml.full.ItemHelper;
import org.intermine.xml.full.ItemFactory;
import org.intermine.ontology.DagParser;
import org.intermine.ontology.OboParser;
import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemWriter;
import org.intermine.metadata.Model;

import org.apache.log4j.Logger;

/**
 * DataConverter to parse a go annotation file into Items
 * @author Andrew Varley
 */
public class GoConverter extends FileConverter
{
    protected static final String GENOMIC_NS = "http://www.flymine.org/model/genomic#";

    protected ItemWrapper product = null;
    protected Map goTerms = new HashMap();
    protected Map databases = new HashMap();
    protected Map publications = new HashMap();
    protected Map organisms = new HashMap();
    protected Map termIdNameMap = new HashMap();
    protected int id = 0;
    protected File ontology;
    protected Map withTypes = new HashMap();
    protected Map annotations = new HashMap();
    protected ItemFactory itemFactory;

    private static final Logger LOG = Logger.getLogger(GoConverter.class);

    /**
     * Constructor
     * @param writer the ItemWriter used to handle the resultant items
     * @throws Exception if an error occurs in storing or finding Model
     */
    public GoConverter(ItemWriter writer) throws Exception {
        super(writer);
        itemFactory = new ItemFactory(Model.getInstanceByName("genomic"));
        addWithType("FB", "Gene", "organismDbId");
        addWithType("UniProt", "Protein", "primaryAccession");
    }

    /**
     * Set the GO ontology file to be read from DAG format.
     * @param ontology the GO ontology file
     */
    public void setOntology(File ontology) {
        this.ontology = ontology;
    }

    /**
     * @see FileConverter#process
     */
    public void process(Reader reader) throws ObjectStoreException, IOException {
        try {
            if (ontology.getName().endsWith(".ontology")) {
                termIdNameMap = new DagParser().getTermIdNameMap(new FileReader(ontology));
            } else if (ontology.getName().endsWith(".obo")) {
                termIdNameMap = new OboParser().getTermIdNameMap(new FileReader(ontology));
            } else {
                throw new IllegalArgumentException("Don't know how to deal with ontology file"
                        + ontology.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new IOException("" + e);
        }
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("!")) {
                continue;
            }
            String[] array = line.split("\t", -1); //keep trailing empty Strings

            String qualifier = array[3];
            String goEvidence = array[6];
            String productId = array[1];
            String goId = array[4];

            GoTermProduct key = new GoTermProduct(productId, goId, goEvidence, qualifier);

            Item annotation = (Item) annotations.get(key);
            if (!annotations.containsKey(key)) {
                annotation = newAnnotation(qualifier,
                                                newDatabase(array[14]),
                                                newPublication(array[5]),
                                                goEvidence,
                                                newProduct(productId, array[11],
                                                           newOrganism(array[12])),
                                                newGoTerm(goId),
                                                array[7]);
                annotations.put(key, annotation);
            } else {
                Item pub = newPublication(array[5]);
                if (pub != null) {
                    annotation.addToCollection("evidence", pub.getIdentifier());
                }
            }
            writer.store(ItemHelper.convert(annotation));
        }
    }

    /**
     * @see FileConverter#close
     */
    public void close() throws ObjectStoreException {
        store(goTerms.values());
        store(databases.values());
        store(publications.values());
        store(organisms.values());
    }

    /**
     * Create a new annotation item linking a product with a term, with evidence code, database and
     * publication
     * @param qualifier qualifier (eg NOT) or null
     * @param database the database
     * @param publication the publication
     * @param goEvidence the goEvidence
     * @param product the product
     * @param goTerm the goTerm
     * @param withText String from the 'with' column of gene_associationfile
     * @return the annotation
     * @throws ObjectStoreException if problem storing 'with' BioEntities
     */
    protected Item newAnnotation(String qualifier, Item database, Item publication,
                                 String goEvidence, Item product, Item goTerm,
                                 String withText) throws ObjectStoreException {
        Item item = createItem("GOAnnotation");
        if (!"".equals(qualifier)) {
            item.addAttribute(new Attribute("qualifier", qualifier));
        }
        String goId = goTerm.getAttribute("identifier").getValue();
        item.setAttribute("identifier", goId);
        if (termIdNameMap.containsKey(goId)) {
            item.setAttribute("name", (String) termIdNameMap.get(goId));
        }
        item.setAttribute("evidenceCode", goEvidence);
        item.setReference("subject", product.getIdentifier());
        item.setReference("property", goTerm.getIdentifier());
        if (!"".equals(withText)) {
            item.setAttribute("withText", withText);
            List with = createWithObjects(withText);
            if (with.size() != 0) {
                List idList = new ArrayList();
                Iterator withIter = with.iterator();
                while (withIter.hasNext()) {
                    Item withObject = (Item) withIter.next();
                    writer.store(ItemHelper.convert(withObject));
                    idList.add(withObject.getIdentifier());
                }
                item.addCollection(new ReferenceList("with", idList));
            }
        }

        ReferenceList references = new ReferenceList();
        references.setName("evidence");
        references.addRefId(database.getIdentifier());
        if (publication != null) {
            references.addRefId(publication.getIdentifier());
        }
        item.addCollection(references);
        return item;
    }


    private void addWithType(String prefix, String clsName, String fieldName) {
        withTypes.put(prefix, new WithType(clsName, fieldName));
    }



    /**
     * Given the 'with' text from a gene_association entry parse for recognised identifier
     * types and create Gene or Protein items accordingly.
     * @param withText string from the gene_association entry
     * @return a list of Items
     */
    protected List createWithObjects(String withText) {
        List with = new ArrayList();
        try {
            String[] elements = withText.split("; |, ");
            for (int i = 0; i < elements.length; i++) {
                String entry = elements[i].trim();
                // rely on the format being type:identifier
                if (entry.indexOf(':') > 0) {
                    String prefix = entry.substring(0, entry.indexOf(':'));
                    String value = entry.substring(entry.indexOf(':') + 1);

                    if (withTypes.containsKey(prefix)) {

                        WithType wt = (WithType) withTypes.get(prefix);
                        Item item = createItem(wt.clsName);
                        item.setAttribute(wt.fieldName, value);
                        with.add(item);

                    }
                }
            }
        } catch (RuntimeException e) {
            LOG.error("createWithObjects broke with: " + withText);
            throw e;
        }
        return with;
    }


    /**
     * Create a new product of a certain type (gene or protein) of a certain organism
     * @param identifier the identifier
     * @param type the type
     * @param organism the organism
     * @return the product
     * @throws ObjectStoreException if an error occurs in storing
     */
    protected Item newProduct(String identifier, String type, Item organism)
        throws ObjectStoreException {
        String key = identifier + type + (organism == null ? "" : organism.getIdentifier());
        String idField = null;
        String clsName = null;
        if ("gene".equals(type)) {
            if (organism == null) {
                throw new IllegalArgumentException("Encountered gene without a valid organism");
            }
            clsName = "Gene";
            idField = "organismDbId";

        } else if ("protein".equals(type)) {
            clsName = "Protein";
            idField = "primaryAccession";
        } else {
            LOG.warn("Unrecognised product type '" + type + "'");
        }
        Item item;
        if ((clsName != null) && (product == null || !product.key.equals(key))) {
            item = createItem(clsName);
            item.addAttribute(new Attribute(idField, identifier));
            if (organism != null) {
                item.addReference(new Reference("organism", organism.getIdentifier()));
            }
            writer.store(ItemHelper.convert(item));
            product = new ItemWrapper(key, item);
        } else {
            item = product.item;
        }
        return item;
    }

    /**
     * Create a new go term
     * @param identifier the identifier
     * @return the go term
     */
    protected Item newGoTerm(String identifier) {
        Item item = (Item) goTerms.get(identifier);
        if (item == null) {
            item = createItem("GOTerm");
            item.addAttribute(new Attribute("identifier", identifier));
            goTerms.put(identifier, item);
        }
        return item;
    }

    /**
     * Create a new database given a database code
     * @param code the code
     * @return the database
     */
    protected Item newDatabase(String code) {
        Item item = (Item) databases.get(code);
        if (item == null) {
            item = createItem("Database");
            String title = null;
            if ("UniProt".equals(code)) {
                title = "UniProt";
            } else if ("FB".equals(code)) {
                title = "FlyBase";
            } else if ("WB".equals(code)) {
                title = "WormBase";
            } else if ("SP".equals(code)) {
                title = "Swiss-Prot";
            } else if ("MGI".equals(code)) {
                title = "MGI";
            } else if ("SGD".equals(code)) {
                title = "SGD";
            } else if ("PINC".equals(code)) {
                title = "PINC";
            } else if ("HGNC".equals(code)) {
                title = "HGNC";
            } else {
                throw new IllegalArgumentException("Database with code '" + code
                                                   + "' not recognised");
            }
            item.addAttribute(new Attribute("title", title));
            databases.put(code, item);
        }
        return item;
    }


    /**
     * Create a new publication given list of codes
     * @param codes the codes
     * @return the publication
     */
    protected Item newPublication(String codes) {
        Item item = null;
        String[] array = codes.split("[|]");
        for (int i = 0; i < array.length; i++) {
            if (array[i].startsWith("PMID:")) {
                String code = array[i].substring(5);
                item = (Item) publications.get(code);
                if (item == null) {
                    item = createItem("Publication");
                    item.addAttribute(new Attribute("pubMedId", code));
                    publications.put(code, item);
                }
                break;
            }
        }
        return item;
    }

    /**
     * Create a new organism given a taxonomy id
     * @param taxonId the taxonId
     * @return the organism
     */
    protected Item newOrganism(String taxonId) {
        if (taxonId.equals("taxon:")) {
            return null;
        }
        taxonId = taxonId.split(":")[1];
        Item item = (Item) organisms.get(taxonId);
        if (item == null) {
            item = createItem("Organism");
            item.addAttribute(new Attribute("taxonId", taxonId));
            organisms.put(taxonId, item);
        }
        return item;
    }

    /**
     * Convenience method for creating a new Item
     * @param className the name of the class
     * @return a new Item
     */
    protected Item createItem(String className) {
        return itemFactory.makeItem(alias(className) + "_" + (id++), GENOMIC_NS + className, "");
    }

    /**
     * Class to identify an Item using a unique key
     */
    class ItemWrapper
    {
        String key;
        Item item;

        /**
         * Constructor
         * @param key the key
         * @param item the Item
         */
        ItemWrapper(String key, Item item) {
            this.key = key;
            this.item = item;
        }
    }

    /**
     * Identify a GoTerm/product pair with evidence code and qualifier
     */
    class GoTermProduct
    {
        String productId;
        String goId;
        String code;
        String qualifier;

        /**
         * Constructor
         * @param productId gene/protein identifier
         * @param goId GO term id
         * @param code evidence code
         * @param qualifier qualifier
         */
        GoTermProduct(String productId, String goId, String code, String qualifier) {
            this.productId = productId;
            this.goId = goId;
            this.code = code;
            this.qualifier = qualifier;
        }

        /**
         * @see Object#equals
         */
        public boolean equals(Object o) {
            if (o instanceof GoTermProduct) {
                GoTermProduct go = (GoTermProduct) o;
                return productId.equals(go.productId)
                    && goId.equals(go.goId)
                    && code.equals(go.code)
                    && qualifier.equals(go.qualifier);
            }
            return false;
        }

        /**
         * @see Object#hashCode
         */
        public int hashCode() {
            return (3 * productId.hashCode())
                + (5 * goId.hashCode())
                + (7 * code.hashCode())
                + (11 * qualifier.hashCode());
        }
    }

    /**
     * Class to hold information about a BioEntity item to create for a particular
     * identifier prefix in the gene_association 'with' column'.
     */
    class WithType
    {
        String clsName;
        String fieldName;

        /**
         * Constructor
         * @param clsName the classname
         * @param fieldName name of field to set
         */
        WithType(String clsName, String fieldName) {
            this.clsName = clsName;
            this.fieldName = fieldName;
        }
    }
}
