package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.util.DynamicUtil;

import org.flymine.model.genomic.Chromosome;
import org.flymine.model.genomic.DataSet;
import org.flymine.model.genomic.DataSource;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.IntergenicRegion;
import org.flymine.model.genomic.Location;
import org.flymine.model.genomic.Synonym;

import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Methods for creating feature for intergenic regions.
 * @author Kim Rutherford
 */
public class IntergenicRegionUtil
{
    private ObjectStoreWriter osw = null;
    private ObjectStore os;
    private DataSet dataSet;
    private DataSource dataSource;
    
    /**
     * Create a new IntergenicRegionUtil object that will operate on the given ObjectStoreWriter.
     * @param osw
     */
    public IntergenicRegionUtil(ObjectStoreWriter osw) {
        this.osw = osw;
        this.os = osw.getObjectStore();
        dataSource = (DataSource) DynamicUtil.createObject(Collections.singleton(DataSource.class));
        dataSource.setName("FlyMine");
        try {
            dataSource = (DataSource) os.getObjectByExample(dataSource, Collections.singleton("name"));
        } catch (ObjectStoreException e) {
            throw new RuntimeException("unable to fetch FlyMine DataSource object", e);
        }
    }

    /**
     * Create IntergenicRegion objects 
     * @throws ObjectStoreException
     */
    public void createIntergenicRegionFeatures() throws ObjectStoreException {
        Results results =
            PostProcessUtil.findLocations(os, Chromosome.class, Gene.class);
        results.setBatchSize(500);

        dataSet = (DataSet) DynamicUtil.createObject(Collections.singleton(DataSet.class));
        dataSet.setTitle("FlyMine intergenic regions");
        dataSet.setDescription("Intergenic regions created by FlyMine");
        dataSet.setVersion("" + new Date()); // current time and date
        dataSet.setUrl("http://www.flymine.org");
        dataSet.setDataSource(dataSource);
                              
        Iterator resIter = results.iterator();

        Integer previousChrId = null;
        Set locationSet = new HashSet();
        
        while (resIter.hasNext()) {
            ResultsRow rr = (ResultsRow) resIter.next();
            Integer chrId = (Integer) rr.get(0);
            Location loc = (Location) rr.get(1);

            if (previousChrId == null || chrId.equals(previousChrId)) {
                locationSet.add(loc);
            } else {
                Iterator irIter = createIntergenicRegionFeatures(locationSet, previousChrId);
                storeItergenicRegions(osw, irIter);
                locationSet = new HashSet();
            }
            previousChrId = chrId;
        }
        
        if (previousChrId != null) {
            Iterator irIter = createIntergenicRegionFeatures(locationSet, previousChrId);
            storeItergenicRegions(osw, irIter);
            
            // we've created some IntergenicRegion objects so store() the DataSet
            osw.store(dataSet);
        }
    }

    /**
     * Store the objects returned by createIntergenicRegionFeatures().
     */
    private void storeItergenicRegions(ObjectStoreWriter objectStoreWriter, Iterator irIter) 
        throws ObjectStoreException {
        while (irIter.hasNext()) {
            IntergenicRegion ir = (IntergenicRegion) irIter.next();
            objectStoreWriter.store(ir);
            objectStoreWriter.store(ir.getChromosomeLocation());
            objectStoreWriter.store((InterMineObject) ir.getSynonyms().iterator().next());
        }
    }

    /**
     * Return an iterator over a Set of IntergenicRegion objects that don't overlap the Locations 
     * in the locationSet argument.  The caller must call ObjectStoreWriter.store() on the 
     * IntergenicRegion, its chromosomeLocation and the synonym in the synonyms collection.
     * @param locationSet a set of Locations for the Genes on a particular chromosome
     * @param chrId the ID of the Chromosome that the Locations refer to
     * @return an Iterator over IntergenicRegion objects
     * @throws ObjectStoreException if there is an ObjectStore problem
     */
    protected Iterator createIntergenicRegionFeatures(Set locationSet, Integer chrId) 
        throws ObjectStoreException {
        final Chromosome chr = (Chromosome) os.getObjectById(chrId);
        final BitSet bs = new BitSet(chr.getLength().intValue() + 1);
        
        Iterator locationIter = locationSet.iterator();
        
        while(locationIter.hasNext()) {
            Location location = (Location) locationIter.next();
            
            bs.set(location.getStart().intValue(), location.getEnd().intValue() + 1);
        }
        
        return new Iterator() {
            int prevEndPos = 0;
            
            {
                if (bs.nextClearBit(prevEndPos) == -1) {
                    prevEndPos = -1;
                }
            }
            
            public boolean hasNext() {
                return prevEndPos != -1;
            }

            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                
                int nextIntergenicStart = bs.nextClearBit(prevEndPos + 1);
                int intergenicEnd;
                int nextSetBit = bs.nextSetBit(nextIntergenicStart);

                if (nextSetBit == -1 
                    || bs.nextClearBit(nextSetBit) > chr.getLength().intValue()) {
                    intergenicEnd = chr.getLength().intValue();
                    prevEndPos = -1;
                } else {
                    intergenicEnd = nextSetBit - 1;
                    prevEndPos = intergenicEnd;
                }

                
                IntergenicRegion intergenicRegion = (IntergenicRegion) 
                    DynamicUtil.createObject(Collections.singleton(IntergenicRegion.class));
                Location location = 
                    (Location) DynamicUtil.createObject(Collections.singleton(Location.class));
                Synonym synonym = 
                    (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
                location.setStart(new Integer(nextIntergenicStart));
                location.setEnd(new Integer(intergenicEnd));
                location.setStrand(new Integer(1));
                location.setPhase(new Integer(0));
                location.setStartIsPartial(Boolean.FALSE);
                location.setEndIsPartial(Boolean.FALSE);
                location.setSubject(intergenicRegion);
                location.setObject(chr);
                location.addEvidence(dataSet);
                intergenicRegion.setChromosomeLocation(location);
                intergenicRegion.setChromosome(chr);
                intergenicRegion.setOrganism(chr.getOrganism());
                intergenicRegion.addSynonyms(synonym);
                intergenicRegion.addEvidence(dataSet);
                synonym.addEvidence(dataSet);
                synonym.setSource(dataSource);
                synonym.setSubject(intergenicRegion);
                synonym.setType("identifier");
                int length = location.getEnd().intValue() - location.getStart().intValue() + 1;
                intergenicRegion.setLength(new Integer(length));
                
                String identifier = "integenic_region_chr" + chr.getIdentifier()
                    + "_" + location.getStart() + ".." + location.getEnd();
                intergenicRegion.setIdentifier(identifier);
                
                synonym.setValue(intergenicRegion.getIdentifier());

                return intergenicRegion;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove() not implemented");
            }
        };
    }   
}