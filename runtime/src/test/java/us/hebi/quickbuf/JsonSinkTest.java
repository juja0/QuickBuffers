/*-
 * #%L
 * quickbuf-runtime
 * %%
 * Copyright (C) 2019 - 2020 HEBI Robotics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package us.hebi.quickbuf;

import com.google.protobuf.util.JsonFormat;
import com.google.quickbuf.Struct;
import com.google.quickbuf.Value;
import org.junit.Test;
import protos.test.quickbuf.ForeignEnum;
import protos.test.quickbuf.RootMessageOuterClass;
import protos.test.quickbuf.TestAllTypes;
import protos.test.quickbuf.TestAllTypes.NestedEnum;
import protos.test.quickbuf.external.ImportEnum;

import java.io.IOException;
import java.util.Base64;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Florian Enner
 * @since 13 Jul 2020
 */
public class JsonSinkTest {

    @Test
    public void testEnumNumbers() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.setOptionalNestedEnumValue(NestedEnum.BAR_VALUE);
        msg.setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR);
        msg.setOptionalImportEnum(ImportEnum.IMPORT_BAZ);

        String desired = "{\"optionalNestedEnum\":2,\"optionalForeignEnum\":5,\"optionalImportEnum\":9}";
        String result = newJsonSink().setWriteEnumsAsInts(true).writeMessage(msg).toString();
        assertEquals(desired, result);
    }

    @Test
    public void testEnumStrings() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.setOptionalNestedEnumValue(NestedEnum.BAR_VALUE);
        msg.setOptionalForeignEnum(ForeignEnum.FOREIGN_BAR);
        msg.setOptionalImportEnum(ImportEnum.IMPORT_BAZ);

        String desired = "{\"optionalNestedEnum\":\"BAR\",\"optionalForeignEnum\":\"FOREIGN_BAR\",\"optionalImportEnum\":\"IMPORT_BAZ\"}";
        String result = newJsonSink().setWriteEnumsAsInts(false).writeMessage(msg).toString();
        assertEquals(desired, result);
    }

    @Test
    public void testRepeatedEnums() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableRepeatedNestedEnum().addAll(NestedEnum.FOO, NestedEnum.BAR, NestedEnum.BAZ, NestedEnum.BAZ);

        assertEquals("{\"repeatedNestedEnum\":[\"FOO\",\"BAR\",\"BAZ\",\"BAZ\"]}",
                newJsonSink().setWriteEnumsAsInts(false).writeMessage(msg).toString());

        assertEquals("{\"repeatedNestedEnum\":[1,2,3,3]}",
                newJsonSink().setWriteEnumsAsInts(true).writeMessage(msg).toString());
    }

    @Test
    public void testBytes() throws IOException {
        byte[] randomBytes = new byte[31];
        new Random(0).nextBytes(randomBytes);
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalBytes().addAll(randomBytes);
        msg.getMutableRepeatedBytes().add(new byte[0]);
        msg.getMutableRepeatedBytes().add(new byte[]{'A'});
        msg.getMutableRepeatedBytes().add(new byte[]{'A', 'B'});
        msg.getMutableRepeatedBytes().add(new byte[]{'A', 'B', 'C'});
        msg.getMutableRepeatedBytes().add(randomBytes);

        String javaBase64 = Base64.getEncoder().encodeToString(randomBytes);
        String desired = String.format(
                "{\"optionalBytes\":\"%s\",\"repeatedBytes\":[\"\",\"QQ==\",\"QUI=\",\"QUJD\",\"%s\"]}",
                javaBase64, javaBase64);
        assertEquals(desired, newJsonSink().writeMessage(msg).toString());
    }

    @Test
    public void testNestedMessage() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        msg.getMutableOptionalNestedMessage().setBb(2); // with content
        msg.getMutableOptionalForeignMessage(); // empty

        // minimal
        assertEquals("{\"optionalNestedMessage\":{\"bb\":2},\"optionalForeignMessage\":{}}",
                newJsonSink().writeMessage(msg).toString());

        // pretty print
        assertEquals("{\n" +
                "  \"optionalNestedMessage\": {\n" +
                "    \"bb\": 2\n" +
                "  },\n" +
                "  \"optionalForeignMessage\": {\n" +
                "  }\n" +
                "}", msg.toString());
    }

    @Test
    public void testEmptyMessage() throws IOException {
        TestAllTypes msg = TestAllTypes.newInstance();
        assertEquals("{}", newJsonSink().writeMessage(msg).toString());
        assertEquals("{\n}", newJsonSinkPretty().writeMessage(msg).toString());
    }

    @Test
    public void testPrettyOutput() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());

        // add some empty messages and arrays
        msg.getMutableOptionalImportMessage();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedBytes().next();
        msg.getMutableRepeatedDouble().addAll(new double[]{Double.NaN, Double.NEGATIVE_INFINITY, 0.0, -28.3d});
        msg.getMutableRepeatedFloat().clear();

        // Copied from https://jsonformatter.org/json-parser
        String desired = "{\n" +
                "  \"optionalDouble\": 100,\n" +
                "  \"optionalFixed64\": 103,\n" +
                "  \"optionalSfixed64\": 105,\n" +
                "  \"optionalInt64\": 109,\n" +
                "  \"optionalUint64\": 111,\n" +
                "  \"optionalSint64\": 107,\n" +
                "  \"optionalFloat\": 101,\n" +
                "  \"optionalFixed32\": 102,\n" +
                "  \"optionalSfixed32\": 104,\n" +
                "  \"optionalInt32\": 108,\n" +
                "  \"optionalUint32\": 110,\n" +
                "  \"optionalSint32\": 106,\n" +
                "  \"optionalNestedEnum\": \"FOO\",\n" +
                "  \"optionalForeignEnum\": \"FOREIGN_BAR\",\n" +
                "  \"optionalImportEnum\": \"IMPORT_BAZ\",\n" +
                "  \"optionalBool\": true,\n" +
                "  \"optionalNestedMessage\": {\n" +
                "    \"bb\": 2\n" +
                "  },\n" +
                "  \"optionalForeignMessage\": {\n" +
                "    \"c\": 3\n" +
                "  },\n" +
                "  \"optionalImportMessage\": {\n" +
                "  },\n" +
                "  \"optionalgroup\": {\n" +
                "    \"a\": 4\n" +
                "  },\n" +
                "  \"optionalBytes\": \"dXRmOPCfkqk=\",\n" +
                "  \"defaultBytes\": \"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\n" +
                "  \"optionalString\": \"optionalString\uD83D\uDCA9\",\n" +
                "  \"optionalCord\": \"hello!\",\n" +
                "  \"repeatedDouble\": [\"NaN\", \"-Infinity\", 0, -28.3],\n" +
                "  \"repeatedFloat\": [],\n" +
                "  \"repeatedInt32\": [-2, -1, 0, 1, 2, 3, 4, 5],\n" +
                "  \"repeatedPackedInt32\": [-1, 0, 1, 2, 3, 4, 5],\n" +
                "  \"repeatedForeignMessage\": [{\n" +
                "    \"c\": 0\n" +
                "  }, {\n" +
                "    \"c\": 1\n" +
                "  }, {\n" +
                "    \"c\": 2\n" +
                "  }, {\n" +
                "  }, {\n" +
                "  }],\n" +
                "  \"repeatedgroup\": [{\n" +
                "    \"a\": 3\n" +
                "  }, {\n" +
                "    \"a\": 4\n" +
                "  }],\n" +
                "  \"repeatedBytes\": [\"YXNjaWk=\", \"dXRmOPCfkqk=\", \"YXNjaWk=\", \"dXRmOPCfkqk=\", \"\"],\n" +
                "  \"repeatedString\": [\"hello\", \"world\", \"ascii\", \"utf8\uD83D\uDCA9\"]\n" +
                "}";
        assertEquals(desired, msg.toString());
    }

    @Test
    public void testMiniOutput() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());

        // add some empty messages and arrays
        msg.getMutableOptionalImportMessage();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedBytes().next();
        msg.getMutableRepeatedDouble().addAll(new double[]{Double.NaN, Double.NEGATIVE_INFINITY, 0.0, -28.3d});
        msg.getMutableRepeatedFloat().clear();

        assertEquals(miniOutputResult, newJsonSink().setPreserveProtoFieldNames(false).setWriteEnumsAsInts(false).writeMessage(msg).toString());
    }

    // Copied from https://codebeautify.org/jsonminifier
    protected String miniOutputResult = "{\"optionalDouble\":100,\"optionalFixed64\":103,\"optionalSfixed64\":105,\"optionalInt64\":109,\"optionalUint64\":111,\"optionalSint64\":107,\"optionalFloat\":101,\"optionalFixed32\":102,\"optionalSfixed32\":104,\"optionalInt32\":108,\"optionalUint32\":110,\"optionalSint32\":106,\"optionalNestedEnum\":\"FOO\",\"optionalForeignEnum\":\"FOREIGN_BAR\",\"optionalImportEnum\":\"IMPORT_BAZ\",\"optionalBool\":true,\"optionalNestedMessage\":{\"bb\":2},\"optionalForeignMessage\":{\"c\":3},\"optionalImportMessage\":{},\"optionalgroup\":{\"a\":4},\"optionalBytes\":\"dXRmOPCfkqk=\",\"defaultBytes\":\"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\"optionalString\":\"optionalString\uD83D\uDCA9\",\"optionalCord\":\"hello!\",\"repeatedDouble\":[\"NaN\",\"-Infinity\",0,-28.3],\"repeatedFloat\":[],\"repeatedInt32\":[-2,-1,0,1,2,3,4,5],\"repeatedPackedInt32\":[-1,0,1,2,3,4,5],\"repeatedForeignMessage\":[{\"c\":0},{\"c\":1},{\"c\":2},{},{}],\"repeatedgroup\":[{\"a\":3},{\"a\":4}],\"repeatedBytes\":[\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"\"],\"repeatedString\":[\"hello\",\"world\",\"ascii\",\"utf8\uD83D\uDCA9\"]}";

    @Test
    public void testTopLevelArrayOutput() throws IOException {
        RepeatedMessage<TestAllTypes> array = RepeatedMessage.newEmptyInstance(TestAllTypes.getFactory());
        for (int i = 0; i < 5; i++) {
            array.next().setOptionalInt32(i);
        }

        // Serialization
        String desired = "[{\"optionalInt32\":0},{\"optionalInt32\":1},{\"optionalInt32\":2},{\"optionalInt32\":3},{\"optionalInt32\":4}]";
        assertEquals(desired, newJsonSink().writeRepeatedMessage(array).toString());

        // Deserialization
        RepeatedMessage<TestAllTypes> actual = RepeatedMessage.newEmptyInstance(TestAllTypes.getFactory());
        JsonSource.newInstance(desired).readRepeatedMessage(actual);
        assertEquals(array, actual);

    }

    @Test
    public void testMiniOutputProtoNames() throws IOException {
        TestAllTypes msg = TestAllTypes.parseFrom(CompatibilityTest.getCombinedMessage());

        // add some empty messages and arrays
        msg.getMutableOptionalImportMessage();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedForeignMessage().next();
        msg.getMutableRepeatedBytes().next();
        msg.getMutableRepeatedDouble().addAll(new double[]{Double.NaN, Double.NEGATIVE_INFINITY, 0.0, -28.3d});
        msg.getMutableRepeatedFloat().clear();

        assertEquals(miniOutputProtoNamesResult, newJsonSink().setPreserveProtoFieldNames(true).setWriteEnumsAsInts(false).writeMessage(msg).toString());
    }

    // Copied from https://codebeautify.org/jsonminifier
    protected String miniOutputProtoNamesResult = "{\"optional_double\":100,\"optional_fixed64\":103,\"optional_sfixed64\":105,\"optional_int64\":109,\"optional_uint64\":111,\"optional_sint64\":107,\"optional_float\":101,\"optional_fixed32\":102,\"optional_sfixed32\":104,\"optional_int32\":108,\"optional_uint32\":110,\"optional_sint32\":106,\"optional_nested_enum\":\"FOO\",\"optional_foreign_enum\":\"FOREIGN_BAR\",\"optional_import_enum\":\"IMPORT_BAZ\",\"optional_bool\":true,\"optional_nested_message\":{\"bb\":2},\"optional_foreign_message\":{\"c\":3},\"optional_import_message\":{},\"optionalgroup\":{\"a\":4},\"optional_bytes\":\"dXRmOPCfkqk=\",\"default_bytes\":\"YLQguzhR2dR6y5M9vnA5m/bJLaM68B1Pt3DpjAMl9B0+uviYbacSyCvNTVVL8LVAI8KbYk3p75wvkx78WA+a+wgbEuEHsegF8rT18PHQDC0PYmNGcJIcUFhn/yD2qDNemK+HJThVhrQf7/IFtOBaAAgj94tfj1wCQ5zo9np4HZDL5r8a5/K8QKSXCaBsDjFJm/ApacpC0gPlZrzGlt4I+gECoP0uIzCwlkq7fEQwIN4crQm/1jgf+5Tar7uQxO2RoGE60dxLRwOvhMHWOxqHaSHG1YadYcy5jtE65sCaE/yR4Uki8wHPi8+TQxWmBJ0vB9mD+qkbj05yZey4FafLqw==\",\"optional_string\":\"optionalString\uD83D\uDCA9\",\"optional_cord\":\"hello!\",\"repeated_double\":[\"NaN\",\"-Infinity\",0,-28.3],\"repeated_float\":[],\"repeated_int32\":[-2,-1,0,1,2,3,4,5],\"repeated_packed_int32\":[-1,0,1,2,3,4,5],\"repeated_foreign_message\":[{\"c\":0},{\"c\":1},{\"c\":2},{},{}],\"repeatedgroup\":[{\"a\":3},{\"a\":4}],\"repeated_bytes\":[\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"YXNjaWk=\",\"dXRmOPCfkqk=\",\"\"],\"repeated_string\":[\"hello\",\"world\",\"ascii\",\"utf8\uD83D\uDCA9\"]}";

    @Test
    public void testRepeatedFloat() throws IOException {
        RepeatedFloat floats = RepeatedFloat.newEmptyInstance();
        for (int i = -4; i < 4; i++) {
            floats.add(i / 2f);
        }
        FieldName field = FieldName.forField("data");
        String result = newJsonSink()
                .beginObject()
                .writeRepeatedFloat(field, floats)
                .endObject()
                .toString();
        assertEquals(repeatedFloatResult, result);
    }

	@Test
	public void testJsonSerDe() throws IOException
	{
		String json = "{\"foo\":\"bar\"}";

        /*
            Root message definition

            message RootMessage {
              string foo = 1;
            }
        */

		// ------------------------- protobuf root message ---------------------------------------

		protos.test.protobuf.RootMessageOuterClass.RootMessage.Builder grm = protos.test.protobuf.RootMessageOuterClass.RootMessage.newBuilder();

		// protobuf json deserialization
		JsonFormat.parser().merge(json, grm);

		// wire bytes
		byte[] grmBytes = grm.build().toByteArray();

		// reconstructed proto
		protos.test.protobuf.RootMessageOuterClass.RootMessage cgrm = protos.test.protobuf.RootMessageOuterClass.RootMessage.parseFrom(grmBytes);

		// protobuf json serialization
		String roundTripRm = JsonFormat.printer()
				.print(cgrm)
				.replaceAll(" *", "")
				.replaceAll("\n*", "");

		assertEquals(json, roundTripRm); // this works

		// ------------------------- protobuf struct ---------------------------------------

		com.google.protobuf.Struct.Builder gstruct = com.google.protobuf.Struct.newBuilder();

		// protobuf json deserialization
		JsonFormat.parser().merge(json, gstruct);

		// wire bytes
		byte[] gstructBytes = gstruct.build().toByteArray();

		// reconstructed
		com.google.protobuf.Struct cgstruct = com.google.protobuf.Struct.parseFrom(gstructBytes);

		// protobuf json serialization
		String roundTrip = JsonFormat.printer()
				.print(cgstruct)
				.replaceAll(" *", "")
				.replaceAll("\n*", "");

		assertEquals(json, roundTrip); // this also works

		// ------------------------- quickbuf root message ---------------------------------------

		// quickbuf json deserialization (this works)
		RootMessageOuterClass.RootMessage qrm = RootMessageOuterClass.RootMessage.parseFrom(JsonSource.newInstance(json.getBytes()));

		// this works
		assertEquals(json, JsonSink.newInstance().writeMessageSilent(qrm).toString());

		RootMessageOuterClass.RootMessage qcrm = RootMessageOuterClass.RootMessage.newInstance().setFoo("bar");

		// quickbuf json serialization (this also works)
		assertEquals(json, JsonSink.newInstance().writeMessageSilent(qcrm).toString());

		// ------------------------- quickbuf struct ---------------------------------------

		// quickbuf json deserialization <------ fails here
		Struct qstruct = Struct.parseFrom(JsonSource.newInstance(json.getBytes()));

		// doesn't get here
		assertEquals(json, JsonSink.newInstance().writeMessageSilent(qstruct).toString());

		Struct qcstruct = Struct.newInstance();

		qcstruct.addFields(
				Struct.FieldsEntry.newInstance().setKey("foo").setValue(Value.newInstance().setStringValue("bar"))
		);

		// quickbuf json serialization <---- assertion fails here
		assertEquals(json, qcstruct.toString());
		
		/*
			Expected 
			
			{"foo":"bar"}
					
			Actual
			
			{
                "fields": [{
                   "value": {
                        "stringValue": "bar"
                    },  
                    "key": "foo"
                }]
			}					
					
		 */
	}

	protected String repeatedFloatResult = "{\"data\":[-2,-1.5,-1,-0.5,0,0.5,1,1.5]}";

    public JsonSink newJsonSink() {
        return JsonSink.newInstance();
    }

    public JsonSink newJsonSinkPretty() {
        return JsonSink.newPrettyInstance();
    }

}
