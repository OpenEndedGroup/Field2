/*
 * Copyright 2011 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fieldnashorn.sourcemap;

import java.io.IOException;


/**
 * A class for mapping source map names to the actual contents. Used
 * when parsing index maps.
 *
 * @author johnlenz@google.com (John Lenz)
 */
public interface SourceMapSupplier {

    /**
     * @param url The URL of the source map.
     * @return The contents of the map associated with the URL
     * @throws java.io.IOException
     */
    String getSourceMap(String url) throws IOException;
}