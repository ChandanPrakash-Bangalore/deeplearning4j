package org.deeplearning4j.models.sequencevectors.serialization;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.reader.ModelUtils;
import org.deeplearning4j.models.embeddings.reader.impl.BasicModelUtils;
import org.deeplearning4j.models.sequencevectors.SequenceVectors;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.models.word2vec.wordstore.inmemory.AbstractCache;
import org.junit.Before;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;

import static org.junit.Assert.*;

public class WordVectorSerializerTest {
    private AbstractCache<VocabWord> cache;

    @Before
    public void setUp() throws Exception {
        cache = new AbstractCache.Builder<VocabWord>().build();

        val words = new VocabWord[3];
        words[0] = new VocabWord(1.0, "word");
        words[1] = new VocabWord(2.0, "test");
        words[2] = new VocabWord(3.0, "tester");

        for (int i = 0; i < words.length; ++i) {
            cache.addToken(words[i]);
            cache.addWordToIndex(i, words[i].getLabel());
        }
    }

    @Test
    public void sequenceVectorsCorrect_WhenDeserialized() {

        INDArray syn0 = Nd4j.create(10, 2),
                syn1 = Nd4j.create(10, 2),
                syn1Neg = Nd4j.create(10, 2);
        float[] vector = new float[10];
        syn0.putRow(0, Nd4j.create(vector));
        syn1.putRow(0, Nd4j.create(vector));
        syn1Neg.putRow(0, Nd4j.create(vector));

        InMemoryLookupTable<VocabWord> lookupTable =
                (InMemoryLookupTable<VocabWord>) new InMemoryLookupTable.Builder<VocabWord>()
                        .useAdaGrad(false).cache(cache)
                        .build();

        lookupTable.setSyn0(syn0);
        lookupTable.setSyn1(syn1);
        lookupTable.setSyn1Neg(syn1Neg);

        SequenceVectors<VocabWord> vectors = new SequenceVectors.Builder<VocabWord>(new VectorsConfiguration()).
                vocabCache(cache).
                lookupTable(lookupTable).
                build();
        SequenceVectors<VocabWord> deser = null;
        String json = StringUtils.EMPTY;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WordVectorSerializer.writeSequenceVectors(vectors, baos);
            byte[] bytesResult = baos.toByteArray();
            deser = WordVectorSerializer.readSequenceVectors(new ByteArrayInputStream(bytesResult), true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(vectors.getConfiguration());
        assertEquals(vectors.getConfiguration(), deser.getConfiguration());

        assertEquals(cache.totalWordOccurrences(),deser.vocab().totalWordOccurrences());
        assertEquals(cache.totalNumberOfDocs(), deser.vocab().totalNumberOfDocs());
        assertEquals(cache.numWords(), deser.vocab().numWords());

        for (int i = 0; i < cache.words().size(); ++i) {
            val cached = cache.wordAtIndex(i);
            val restored = deser.vocab().wordAtIndex(i);
            assertNotNull(cached);
            assertEquals(cached, restored);
        }

    }

    @Test
    public void W2V_Correct_WhenDeserialized() {

        INDArray syn0 = Nd4j.create(10, 2),
                syn1 = Nd4j.create(10, 2),
                syn1Neg = Nd4j.create(10, 2);
        float[] vector = new float[10];
        syn0.putRow(0, Nd4j.create(vector));
        syn1.putRow(0, Nd4j.create(vector));
        syn1Neg.putRow(0, Nd4j.create(vector));

        InMemoryLookupTable<VocabWord> lookupTable =
                (InMemoryLookupTable<VocabWord>) new InMemoryLookupTable.Builder<VocabWord>()
                        .useAdaGrad(false).cache(cache)
                        .build();

        lookupTable.setSyn0(syn0);
        lookupTable.setSyn1(syn1);
        lookupTable.setSyn1Neg(syn1Neg);

        SequenceVectors<VocabWord> vectors = new SequenceVectors.Builder<VocabWord>(new VectorsConfiguration()).
                vocabCache(cache).
                lookupTable(lookupTable).
                layerSize(200).
                modelUtils(new BasicModelUtils<VocabWord>()).
                build();
        Word2Vec word2Vec = new Word2Vec.Builder(vectors.getConfiguration())
                .vocabCache(vectors.vocab())
                .lookupTable(lookupTable)
                .layerSize(200)
                .modelUtils(new BasicModelUtils<VocabWord>())
                .build();

        Word2Vec deser = null;
        String json = StringUtils.EMPTY;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WordVectorSerializer.writeWord2Vec(word2Vec, baos);
            byte[] bytesResult = baos.toByteArray();
            deser = WordVectorSerializer.readWord2Vec(new ByteArrayInputStream(bytesResult), true);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }

        assertNotNull(word2Vec.getConfiguration());
        assertEquals(word2Vec.getConfiguration(), deser.getConfiguration());

        assertEquals(cache.totalWordOccurrences(),deser.vocab().totalWordOccurrences());
        assertEquals(cache.totalNumberOfDocs(), deser.vocab().totalNumberOfDocs());
        assertEquals(cache.numWords(), deser.vocab().numWords());

        for (int i = 0; i < cache.words().size(); ++i) {
            val cached = cache.wordAtIndex(i);
            val restored = deser.vocab().wordAtIndex(i);
            assertNotNull(cached);
            assertEquals(cached, restored);
        }

    }
}
