/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.transform.process;

import org.apache.inlong.sdk.transform.decode.SourceDecoderFactory;
import org.apache.inlong.sdk.transform.encode.SinkEncoderFactory;
import org.apache.inlong.sdk.transform.pojo.CsvSourceInfo;
import org.apache.inlong.sdk.transform.pojo.FieldInfo;
import org.apache.inlong.sdk.transform.pojo.KvSinkInfo;
import org.apache.inlong.sdk.transform.pojo.TransformConfig;
import org.apache.inlong.sdk.transform.process.converter.DoubleConverter;
import org.apache.inlong.sdk.transform.process.converter.LongConverter;
import org.apache.inlong.sdk.transform.process.converter.TypeConverter;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * TestArithmeticFunctionsTransformProcessor
 * description: test the arithmetic functions in transform processor
 */
public class TestTransformExpressionOperatorsProcessor {

    private static final List<FieldInfo> srcFields = new ArrayList<>();
    private static final List<FieldInfo> dstFields = new ArrayList<>();
    private static final CsvSourceInfo csvSource;
    private static final KvSinkInfo kvSink;

    static {
        srcFields.add(new FieldInfo("numeric1", new DoubleConverter()));
        srcFields.add(new FieldInfo("string2", TypeConverter.DefaultTypeConverter()));
        srcFields.add(new FieldInfo("numeric3", new DoubleConverter()));
        srcFields.add(new FieldInfo("numeric4", new LongConverter()));

        FieldInfo field = new FieldInfo();
        field.setName("result");
        dstFields.add(field);
        csvSource = new CsvSourceInfo("UTF-8", '|', '\\', srcFields);
        kvSink = new KvSinkInfo("UTF-8", dstFields);
    }

    @Test
    public void testEqualsToOperator() throws Exception {
        String transformSql = "select if(string2 = 4,1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|4a|4|8"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|4a|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=0");
        // case2: "3.14159265358979323846|4|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=1");

        transformSql = "select if(numeric3 = 4,1,0) from source";
        config = new TransformConfig(transformSql);
        // case3: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output3 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=1");
        // case4: "3.14159265358979323846|4|4.2|8"
        List<String> output4 = processor.transform("3.14159265358979323846|4|4.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=0");
    }

    @Test
    public void testNotEqualsToOperator() throws Exception {
        String transformSql = "select if(string2 != 4,1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|4a|4|8"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|4a|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=1");
        // case2: "3.14159265358979323846|4|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=0");

        transformSql = "select if(numeric3 != 4,1,0) from source";
        config = new TransformConfig(transformSql);
        // case3: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output3 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=0");
        // case4: "3.14159265358979323846|4|4.2|8"
        List<String> output4 = processor.transform("3.14159265358979323846|4|4.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=1");
    }

    @Test
    public void testGreaterThanEqualsOperator() throws Exception {
        String transformSql = "select if(string2 >= 4,1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|3a|4|8"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|3a|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=0");
        // case2: "3.14159265358979323846|5|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|5|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=1");

        transformSql = "select if(numeric3 >= 4,1,0) from source";
        config = new TransformConfig(transformSql);
        // case3: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output3 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=1");
        // case4: "3.14159265358979323846|4|3.2|8"
        List<String> output4 = processor.transform("3.14159265358979323846|4|3.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=0");
    }

    @Test
    public void testGreaterThanOperator() throws Exception {
        String transformSql = "select if(string2 > 4.1,1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|3a|4|8"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|3a|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=0");
        // case2: "3.14159265358979323846|5|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|5|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=1");

        transformSql = "select if(numeric3 > 4.1,1,0) from source";
        config = new TransformConfig(transformSql);
        // case3: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output3 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=0");
        // case4: "3.14159265358979323846|4|4.2|8"
        List<String> output4 = processor.transform("3.14159265358979323846|4|4.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=1");
    }

    @Test
    public void testMinorThanEqualsOperator() throws Exception {
        String transformSql = "select if(string2 <= 4,1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|3a|4|8"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|3a|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=1");
        // case2: "3.14159265358979323846|5|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|5|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=0");

        transformSql = "select if(numeric3 <= 4,1,0) from source";
        config = new TransformConfig(transformSql);
        // case3: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output3 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=1");
        // case4: "3.14159265358979323846|4|4.2|8"
        List<String> output4 = processor.transform("3.14159265358979323846|4|4.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=0");
    }

    @Test
    public void testMinorThanOperator() throws Exception {
        String transformSql = "select if(string2 < 4.1,1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|3a|4|8"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|3a|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=1");
        // case2: "3.14159265358979323846|5|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|5|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=0");

        transformSql = "select if(numeric3 < 4,1,0) from source";
        config = new TransformConfig(transformSql);
        // case3: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output3 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=0");
        // case4: "3.14159265358979323846|4|3.2|8"
        List<String> output4 = processor.transform("3.14159265358979323846|4|3.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=1");
    }

    @Test
    public void testNotOperator() throws Exception {
        String transformSql = "select if(!(string2 < 4),1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|3a|4|8"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|3a|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=0");
        // case2: "3.14159265358979323846|5|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|5|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=1");

        transformSql = "select if(!(numeric3 < 3.9),1,0) from source";
        config = new TransformConfig(transformSql);
        // case3: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output3 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=1");
        // case4: "3.14159265358979323846|4|3.2|8"
        List<String> output4 = processor.transform("3.14159265358979323846|4|3.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=0");
    }

    @Test
    public void testOrOperator() throws Exception {
        String transformSql = "select if((string2 < 4) or (numeric4 > 5),1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|3a|4|8"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|3a|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=1");
        // case2: "3.14159265358979323846|5|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|5|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=1");
        // case3: "3.14159265358979323846|5|4|4"
        List<String> output3 = processor.transform("3.14159265358979323846|5|4|4");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=0");

        transformSql = "select if((numeric3 < 4) or (numeric4 > 5),1,0) from source";
        config = new TransformConfig(transformSql);
        // case4: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output4 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=1");
        // case5: "3.14159265358979323846|4|3.2|8"
        List<String> output5 = processor.transform("3.14159265358979323846|4|3.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output5.get(0), "result=1");
        // case6: "3.14159265358979323846|4|4.2|5"
        List<String> output6 = processor.transform("3.14159265358979323846|4|4.2|5");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output6.get(0), "result=0");
    }

    @Test
    public void testAndOperator() throws Exception {
        String transformSql = "select if((string2 < 4) and (numeric4 > 5),1,0) from source";
        TransformConfig config = new TransformConfig(transformSql);
        // case1: "3.14159265358979323846|3a|4|4"
        TransformProcessor<String, String> processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output1 = processor.transform("3.14159265358979323846|3a|4|4");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output1.get(0), "result=0");
        // case2: "3.14159265358979323846|5|4|8"
        List<String> output2 = processor.transform("3.14159265358979323846|5|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output2.get(0), "result=0");
        // case3: "3.14159265358979323846|3|4|8"
        List<String> output3 = processor.transform("3.14159265358979323846|3|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output3.get(0), "result=1");

        transformSql = "select if((numeric3 < 4) and (numeric4 > 5),1,0) from source";
        config = new TransformConfig(transformSql);
        // case4: "3.14159265358979323846|4|4|8"
        processor = TransformProcessor
                .create(config, SourceDecoderFactory.createCsvDecoder(csvSource),
                        SinkEncoderFactory.createKvEncoder(kvSink));
        List<String> output4 = processor.transform("3.14159265358979323846|4|4|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output4.get(0), "result=0");
        // case5: "3.14159265358979323846|4|3.2|4"
        List<String> output5 = processor.transform("3.14159265358979323846|4|3.2|4");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output5.get(0), "result=0");
        // case6: "3.14159265358979323846|4|3.2|8"
        List<String> output6 = processor.transform("3.14159265358979323846|4|3.2|8");
        Assert.assertEquals(1, output1.size());
        Assert.assertEquals(output6.get(0), "result=1");
    }
}