package org.apache.openjpa.jdbc.sql;

import org.apache.openjpa.jdbc.kernel.JDBCStore;
import org.apache.openjpa.jdbc.kernel.exps.Val;
import org.apache.openjpa.kernel.BrokerImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;

import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class TestDBDictionary {

    private DBDictionary dbDictionary;

    private String expected;
    private String name;

    private Class<?> exception;

    private void calculateExpectedResult() {

        if(name == null) {
            this.exception = NullPointerException.class;

        }else if(name.length() == 0 || name=="\"\""){
            this.exception = StringIndexOutOfBoundsException.class;

        }else{
            this.expected = name.replaceAll("([A-Z])", "_$1");
            if (this.expected.startsWith("_")) {
                this.expected = this.expected.substring(1);
            }
            this.expected = this.expected.toLowerCase();
            while(this.expected.contains("__")){
                this.expected = this.expected.replaceAll("__","_");
            }
        }
    }

    public TestDBDictionary(String param) {
        this.name = param;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getTestParameters() {
        Object[][] params = {
                // Prima stesura dei casi di test
                {null},
                {""},
                {"somestring"},
                {"SOMESTRING"},
                {"someOtherString"},
                {"SomeString"},
                {"some_string"},
                {"some_otherString"},
                {"someOther_string"},
                {"someOther_String"},
                //Iterazione JaCoCo
                {"\"some_string\""},
                //Seconda iterazione JaCoCo
                {"some_string\""},
                {"\"some_string"},
                //Iterazione Ba-Dua
                {"\"a\""},
                {"\"a"},
                {"a\""},
                {"a"},
        };
        return Arrays.asList(params);
    }

    @Before
    public void setUp() {
        this.dbDictionary = new DBDictionary();
        calculateExpectedResult();
    }

    @Test
    public void testToSnakeCase() {
        Exception error = null;
        try{
            String value = dbDictionary.toSnakeCase(name);
            Assert.assertEquals(expected, value);

        }catch (Exception e){
            error = e;
        }

        if(error != null){
            Assert.assertEquals(exception, error.getClass());
        }
    }
}
