/*
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
package com.facebook.presto.sql.tree;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class ShowTables
        extends Statement
{
    private final Optional<QualifiedName> schema;
    private final Optional<String> likePattern;

    public ShowTables(Optional<QualifiedName> schema, Optional<String> likePattern)
    {
        checkNotNull(schema, "schema is null");
        checkNotNull(likePattern, "likePattern is null");

        this.schema = schema;
        this.likePattern = likePattern;
    }

    public Optional<QualifiedName> getSchema()
    {
        return schema;
    }

    public Optional<String> getLikePattern()
    {
        return likePattern;
    }

    @Override
    public <R, C> R accept(AstVisitor<R, C> visitor, C context)
    {
        return visitor.visitShowTables(this, context);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(schema, likePattern);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        ShowTables o = (ShowTables) obj;
        return Objects.equals(schema, o.schema) &&
                Objects.equals(likePattern, o.likePattern);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("schema", schema)
                .add("likePattern", likePattern)
                .toString();
    }
}
