﻿/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace Apache.Ignite.Core.Impl.Common
{
    using System.Diagnostics;
    using Apache.Ignite.Core.Binary;
    using Apache.Ignite.Core.Impl.Binary;

    /// <summary>
    /// Maps to PlatformJavaObjectFactoryProxy.
    /// </summary>
    internal class JavaObjectFactoryProxy : IBinaryWriteAware
    {
        /// <summary>
        /// Represents the factory type.
        /// </summary>
        internal enum FactoryType
        {
            User = 0,
            Default = 1
        }

        /** Type code. */
        private readonly int _factoryType;

        /** Java class name */
        private readonly string _factoryClassName;

        /** Optional payload. */
        private readonly object _payload;

        /// <summary>
        /// Initializes a new instance of the <see cref="JavaObjectFactoryProxy"/> class.
        /// </summary>
        /// <param name="type">The type.</param>
        /// <param name="factoryClassName">Name of the factory class.</param>
        /// <param name="payload">The payload.</param>
        public JavaObjectFactoryProxy(FactoryType type, string factoryClassName, object payload)
        {
            _factoryType = (int) type;
            _factoryClassName = factoryClassName;
            _payload = payload;
        }

        /** <inheritdoc /> */
        public void WriteBinary(IBinaryWriter writer)
        {
            var w = writer.GetRawWriter();

            w.WriteInt(_factoryType);
            w.WriteString(_factoryClassName);
            w.WriteObject(_payload);

            w.WriteInt(0);  // TODO: Properties
        }
    }
}
