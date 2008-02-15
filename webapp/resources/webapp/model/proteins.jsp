<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<table width="100%">
  <tr>
    <td valign="top" rowspan="2">
      <div class="heading2">
        Current data
      </div>
      <div class="body">

 <h4>  
  <a href="javascript:toggleDiv('hiddenDiv1');">
    <img id='hiddenDiv1Toggle' src="images/disclosed.gif"/>
      Data from the UniProt Knowledgebase (UniProtKB) ...
  </a>
 </h4>

<div id="hiddenDiv1" class="dataSetDescription">
          <p>
            All proteins from the <a
            href="http://www.ebi.uniprot.org/index.shtml" target="_new">UniProt
            Knowledgebase</a> for the following organisms have
            been loaded:</p>

          <ul>             
              <li><i>D. melanogaster</i></li>
              <li><i>A. gambiae</i></li>
              <li><i>C. elegans</i></li>
              <li><i>H. sapiens</i></li>
              <li><i>M. musculus</i></li>
              <li><i>D. pseudoobscura</i></li>
              <li><i>S. cerevisiae</i></li>
              <li><i>A. mellifera</i></li>
          </ul>
          <p>           
            For each protein record in UniProt for each species the following
            information is extracted and loaded into FlyMine:</p>
         <ul>             
              <li>Entry name</li>
              <li>Primary accession number</li>
              <li>Secondary accession number</li>
              <li>Protein name</li>
              <li>Comments</li>
              <li>Publications</li>
              <li>Sequence</li>
              <li>Gene ORF name</li>
              <li>Protein domain assignments from Interpro - see below</li>
          </ul>     

  </div>
     
   <h4>  
    <a href="javascript:toggleDiv('hiddenDiv2');">
     <img id='hiddenDiv2Toggle' src="images/disclosed.gif"/>
      Data from InterPro ...
    </a>
   </h4>

<div id="hiddenDiv2" class="dataSetDescription">

          <p>
            Protein family and domain assignments to proteins are
            loaded from Uniprot (see above).  Details for each family or domain
            are loaded from <a
            href="http://www.ebi.ac.uk/interpro" target="_new">InterPro</a></p>
          </ul> 
</div>

<%--  // add later:
        <p>
          Search for a protein identifier: <tiles:insert name="browse.tile"/>
        </p>
--%>
      </div>
    </td>

    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download <i>Drosophila</i> data
      </div>
      <div class="body">
        <ul>
          <li>            
              <im:querylink text="<i>D. melanogaster</i> proteins and corresponding genes " skipBuilder="true">
<query name="" model="genomic" view="Protein.identifier Protein.name Protein.primaryAccession Protein.genes.identifier Protein.genes.primaryIdentifier" sortOrder="Protein.identifier Protein.name Protein.primaryAccession Protein.genes.identifier Protein.genes.primaryIdentifier">
  <node path="Protein" type="Protein">
  </node>
  <node path="Protein.organism" type="Organism">
  </node>
  <node path="Protein.organism.name" type="String">
    <constraint op="=" value="Drosophila melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
           </im:querylink>
          </li>

          <li>
             <im:querylink text="<i>D. melanogaster</i> protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.identifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.identifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type">
  <node path="ProteinDomain" type="ProteinDomain">
  </node>
  <node path="ProteinDomain.proteins" type="Protein">
  </node>
  <node path="ProteinDomain.proteins.organism" type="Organism">
  </node>
  <node path="ProteinDomain.proteins.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
             </im:querylink>
          </li>


          <li>              
              <im:querylink text="<i>D. melanogaster</i> proteins with corresponding protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.proteins.name ProteinDomain.proteins.identifier ProteinDomain.identifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.proteins.name ProteinDomain.proteins.identifier ProteinDomain.identifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type">
  <node path="ProteinDomain" type="ProteinDomain">
  </node>
  <node path="ProteinDomain.proteins" type="Protein">
  </node>
  <node path="ProteinDomain.proteins.organism" type="Organism">
  </node>
  <node path="ProteinDomain.proteins.organism.shortName" type="String">
    <constraint op="=" value="D. melanogaster" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
      </im:querylink>
      </li>
        </ul>
      </div>
    </td>
  </tr>

  <tr>
    <td width="40%" valign="top">
      <div class="heading2">
        Bulk download <i>Anopheles</i> data
      </div>
      <div class="body">
        <ul>
          <li>
              <im:querylink text="<i>A. gambiae</i> proteins and corresponding genes " skipBuilder="true">
<query name="" model="genomic" view="Protein.identifier Protein.name Protein.primaryAccession Protein.genes.identifier"> <node path="Protein" type="Protein"> </node> <node path="Protein.organism" type="Organism"> </node> <node path="Protein.organism.name" type="String">   <constraint op="=" value="Anopheles gambiae str. PEST" description="" identifier="" code="A">   </constraint> </node></query> 
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>A. gambiae</i> protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.identifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.identifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type">
  <node path="ProteinDomain" type="ProteinDomain">
  </node>
  <node path="ProteinDomain.proteins" type="Protein">
  </node>
  <node path="ProteinDomain.proteins.organism" type="Organism">
  </node>
  <node path="ProteinDomain.proteins.organism.shortName" type="String">
    <constraint op="=" value="A. gambiae str. PEST" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
              </im:querylink>
          </li>

          <li>
              <im:querylink text="<i>A. gambiae</i> proteins with corresponding protein domains " skipBuilder="true">
<query name="" model="genomic" view="ProteinDomain.proteins.name ProteinDomain.proteins.identifier ProteinDomain.identifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type" sortOrder="ProteinDomain.proteins.name ProteinDomain.proteins.identifier ProteinDomain.identifier ProteinDomain.name ProteinDomain.shortName ProteinDomain.type">
  <node path="ProteinDomain" type="ProteinDomain">
  </node>
  <node path="ProteinDomain.proteins" type="Protein">
  </node>
  <node path="ProteinDomain.proteins.organism" type="Organism">
  </node>
  <node path="ProteinDomain.proteins.organism.shortName" type="String">
    <constraint op="=" value="A. gambiae str. PEST" description="" identifier="" code="A">
    </constraint>
  </node>
</query>
      </im:querylink>
          </li>

        </ul>
      </div>
    </td>
  </tr>
</table>
