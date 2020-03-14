package org.owasp.dependencycheck.data.nvd.ecosystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.owasp.dependencycheck.analyzer.JarAnalyzer;
import org.owasp.dependencycheck.data.nvd.json.CVEJSON40Min11;
import org.owasp.dependencycheck.data.nvd.json.DefCveItem;
import org.owasp.dependencycheck.data.nvd.json.Description;
import org.owasp.dependencycheck.data.nvd.json.LangString;

public class DescriptionEcosystemMapperTest {

    private static final String POSTFIX = ".ecosystem.txt";

    protected static File directory = new File("./src/test/resources/ecosystem");
    
    protected static Map<String, File> getEcosystemFiles() throws IOException {
        if(!directory.exists()) {
            throw new RuntimeException(directory.getCanonicalPath());
        }
        
        File[] listFiles = directory.listFiles( (d, name) -> name.endsWith(POSTFIX));
        
        Map<String, File> map = new HashMap<>();
        for(File file : listFiles) {
            String name = file.getName();
            map.put(name.substring(0, name.length() - POSTFIX.length()), file);
        }
        return map;
    }
    
    @Test
    public void testDescriptionEcosystemMapper() throws IOException {
        DescriptionEcosystemMapper mapper = new DescriptionEcosystemMapper();
        Map<String, File> ecosystemFiles = getEcosystemFiles();
        for (Entry<String, File> entry : ecosystemFiles.entrySet()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(entry.getValue()), StandardCharsets.UTF_8));
            try {
                String description;
                while( (description = bufferedReader.readLine()) != null) {
                    if(description.length() > 0 && !description.startsWith("#")) {
                        String ecosystem = mapper.getEcosystem(asCve(description));
                        if(entry.getKey().equals("null")) {
                            assertNull(description, ecosystem);
                        } else {
                            assertEquals(description, entry.getKey(), ecosystem);
                        }
                    }
                }
            } finally {
                bufferedReader.close();
            }
        }
    }

    @Test
    public void testScoring() throws IOException {
        DescriptionEcosystemMapper mapper = new DescriptionEcosystemMapper();
        String value = "a.cpp b.java c.java";
        assertEquals(JarAnalyzer.DEPENDENCY_ECOSYSTEM, mapper.getEcosystem(asCve(value)));
    }
    
    @Test
    public void testJspLinksDoNotCountScoring() throws IOException {
        DescriptionEcosystemMapper mapper = new DescriptionEcosystemMapper();
        String value = "Read more at https://domain/help.jsp.";
        assertNull(mapper.getEcosystem(asCve(value)));
    }
    
    @Test
    public void testSubsetFileExtensionsDoNotMatch() throws IOException {
        DescriptionEcosystemMapper mapper = new DescriptionEcosystemMapper();
        String value = "Read more at index.html."; // i.e. does not match .h
        assertNull(mapper.getEcosystem(asCve(value)));
    }

    @Test
    public void testSubsetKeywordsDoNotMatch() throws IOException {
        DescriptionEcosystemMapper mapper = new DescriptionEcosystemMapper();
        String value = "Wonder if java senses the gc."; // i.e. does not match 'java se'
        assertNull(mapper.getEcosystem(asCve(value)));
    }

    @Test
    public void testPhpLinksDoNotCountScoring() throws IOException {
        DescriptionEcosystemMapper mapper = new DescriptionEcosystemMapper();
        String value = "Read more at https://domain/help.php.";
        assertNull(mapper.getEcosystem(asCve(value)));
    }    

    private DefCveItem asCve(String description) {
        DefCveItem defCveItem = new DefCveItem();
        
        Description d = new Description();
        
        LangString string = new LangString();
        string.setLang("en");
        string.setValue(description);
        
        d.getDescriptionData().add(string);
        
        CVEJSON40Min11 cve = new CVEJSON40Min11();
        cve.setDescription(d);
        
        defCveItem.setCve(cve);
        
        return defCveItem;
    }
}
