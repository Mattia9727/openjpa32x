package org.apache.openjpa.util;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(value = Parameterized.class)
public class TestCacheMap {
    private CacheMap cacheMap;
    private Object key;
    private Object value;
    private ParamType keyType;
    private int maxCacheMap;

    // Prima iterazione JaCoCo: gestione pinnedMap e softMap
    private boolean inPinnedMap;
    private boolean inSoftMap;

    private enum ParamType {
        NULL,
        VALID,
        EXIST,  //Iterazione JaCoCo
        INVALID
    }

    private void configure(ParamType keyType, ParamType valueType, boolean inPinnedMap, boolean inSoftMap, int maxCacheMap){
        this.keyType = keyType;
        this.inPinnedMap = inPinnedMap;
        this.inSoftMap = inSoftMap;
        this.maxCacheMap = maxCacheMap;
        switch (keyType){
            case NULL:
                this.key = null;
                break;
            case INVALID:
                this.key = new InvalidObject();
                break;
            case EXIST:
                this.key = new Object();
                break;
            case VALID:
                this.key = new Object();
                break;
        }
        switch (valueType){
            case NULL:
                this.value = null;
                break;
            case INVALID:
                this.value = new InvalidObject();
                break;
            case EXIST:
            case VALID:
                this.value = new Object();
                break;
        }
    }

    public TestCacheMap(ParamType keyType, ParamType valueType, boolean inPinnedMap, boolean inSoftMap, int maxCacheMap) {

        configure(keyType, valueType, inPinnedMap, inSoftMap, maxCacheMap);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
//               keyType, valueType, inPinnedMap, inSoftMap, maxCacheMap
                {ParamType.NULL, ParamType.NULL, false, false, 1000},
                {ParamType.NULL, ParamType.INVALID, false, false, 1000},
                {ParamType.NULL, ParamType.VALID, false, false, 1000},
                {ParamType.VALID, ParamType.NULL, false, false, 1000},
                {ParamType.VALID, ParamType.INVALID, false, false, 1000},
                {ParamType.VALID, ParamType.VALID, false, false, 1000},
                {ParamType.INVALID, ParamType.NULL, false, false, 1000},
                {ParamType.INVALID, ParamType.INVALID, false, false, 1000},
                {ParamType.INVALID, ParamType.VALID, false, false, 1000},
                //Prima iterazione JaCoCo: esistenza chiave in CacheMap
                {ParamType.EXIST, ParamType.NULL, false, false, 1000},
                {ParamType.EXIST, ParamType.INVALID, false, false, 1000},
                {ParamType.EXIST, ParamType.VALID, false, false, 1000},
                //Seconda iterazione JaCoCo: esistenza chiave in pinnedMap e/o softMap
                {ParamType.EXIST, ParamType.VALID, false, true, 1000},
                {ParamType.EXIST, ParamType.VALID, true, false, 1000},
                {ParamType.EXIST, ParamType.VALID, true, true, 1000},
                {ParamType.NULL, ParamType.NULL, true, false, 1000},
                {ParamType.VALID, ParamType.VALID, false, true, 1000},
                //Terza iterazione JaCoCo: aggiunta maxCacheMap per test con max=0
                {ParamType.VALID, ParamType.VALID, false, false, 0},
        });
    }

    //Prima iterazione JaCoCo: inserisco oggetto in anticipo tramite PUT (simulo preesistenza dell'oggetto)
    @Before
    public void setup() {
        this.cacheMap = new CacheMap(false,maxCacheMap);
        if (this.keyType == ParamType.EXIST) this.cacheMap.put(this.key, this.value);
        if (this.inPinnedMap) this.cacheMap.pinnedMap.put(this.key, this.value);
        if (this.inSoftMap) this.cacheMap.softMap.put(this.key, this.value);
    }

    @Test
    public void put() throws InterruptedException {
        int nObjectsInCacheMapBeforePut = this.cacheMap.size();                         //Prima iterazione PIT

        Object ret = this.cacheMap.put(this.key, this.value);
        Object getRet =  this.cacheMap.get(this.key);

        //Seconda iterazione PIT: uso di thread con Future per kill di mutante relativo al lock
        AtomicBoolean releasedLockInThread = new AtomicBoolean(false);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Void> future = executor.submit(() -> {
            this.cacheMap.readLock();
            this.cacheMap.readUnlock();
            releasedLockInThread.set(true);
            return null;
        });
        try {
            future.get(1000, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            future.cancel(true);
            Assert.fail("Timeout o eccezione durante l'attesa dell'operazione");
        }
        Assert.assertTrue(releasedLockInThread.get());
        //Fine seconda iterazione PIT

        if(keyType != ParamType.EXIST) nObjectsInCacheMapBeforePut++;                   //Prima iterazione PIT
        if(keyType != ParamType.EXIST && inSoftMap) nObjectsInCacheMapBeforePut--;      //Prima iterazione PIT
        int nObjectsInCacheMapAfterPut = this.cacheMap.size();                          //Prima iterazione PIT
        Assert.assertEquals(nObjectsInCacheMapBeforePut,nObjectsInCacheMapAfterPut);    //Prima iterazione PIT


        System.out.println(getRet);

        if (maxCacheMap == 0){
            Assert.assertNull(this.value);
        }
        else{
            Assert.assertEquals(this.value,getRet);
            if(this.keyType == ParamType.EXIST){
                Assert.assertEquals(this.value, ret);
            }
        }
    }

    @After
    public void tearDown() {
        if (this.cacheMap.get(this.key) != null) this.cacheMap.remove(this.key);
    }
}
