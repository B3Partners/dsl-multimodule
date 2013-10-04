package nl.b3p.geotools.data.linker.blocks;

import nl.b3p.geotools.data.linker.feature.EasyFeature;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Boy de Wit
 */
public class Action_XY_Intersects_Add_Mapped_Attrib extends Action {
    
    protected static final Log logger = LogFactory.getLog(Action_XY_Intersects_Add_Mapped_Attrib.class);    
    private static final String NEW_TABLE_SUFFIX = "_MATCHED";
    
    private String polyTable;
    private String copyColumns;
    private String mappingFile;
    private String sourceMappedColumn;
    private String polyTableMappedColumn;

    public Action_XY_Intersects_Add_Mapped_Attrib() {
    }
    
    public Action_XY_Intersects_Add_Mapped_Attrib(String polyTable, String copyColumns, String mappingFile,
            String sourceMappedColumn, String polyTableMappedColumn) {

        this.polyTable = polyTable;
        this.copyColumns = copyColumns;
        this.mappingFile = mappingFile;
        this.sourceMappedColumn = sourceMappedColumn;
        this.polyTableMappedColumn = polyTableMappedColumn;
    }

    public EasyFeature execute(EasyFeature feature) throws Exception {
        
        polyTable = "BOR_V";
        copyColumns = "kopp_id_admin, mapid, x, y, ocode";
        mappingFile = "mapping.xls";
        sourceMappedColumn = "OCODE";
        polyTableMappedColumn = "LAYER";
        
        /* Dxf is ingelezen naar BOR_V en nu zijn we gm_koppel aan het inlezen */
        
        /* Laden Mapping bestand met OCODE en LAYER Levels */
        
        /* Intersects query op vlakken tabel met filter voor OCODE > LAYER */
        
        /* Zo ja dan nieuw vlak feature maken met aantal attrib velden van gm_koppel
         KOPP_ID_ADMIN, MAPID, X, Y, OCODE */
        
        /* Hier kunnen daarna views op gemaakt worden */

        return feature;
    }

    public String toString() {
        return "";
    }

    public String getDescription_NL() {
        return "Verrijken met attributen na intersects en mapping match..";
    }

   
}
