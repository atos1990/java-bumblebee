// Copyright 2009 The original authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlecode.bumblebee.dto.el.parser;

import com.googlecode.bumblebee.beans.BeanUtil;
import static net.sf.jdpa.cg.Code.*;
import net.sf.jdpa.cg.model.Expression;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.ByteArrayInputStream;

/**
 * @author Andreas Nilsson
 */
public class DTOELParserTest {

    @Test
    public void testParseProperty() throws Exception {
        assertEquals(call("getProperty").of(BeanUtil.class).with($(0), constant("foo")), parse("foo"));
    }

    @Test
    public void testParsePropertyPath() throws Exception {
        assertEquals(call("getProperty").of(BeanUtil.class).with(
                call("getProperty").of(BeanUtil.class).with($(0), constant("foo")), constant("bar")
        ), parse("foo.bar"));
    }

    protected Expression parse(String text) throws Exception {
        DTOELParser parser = new DTOELParser(new ByteArrayInputStream(text.getBytes()), "UTF-8");
        return parser.Expression();
    }

}
