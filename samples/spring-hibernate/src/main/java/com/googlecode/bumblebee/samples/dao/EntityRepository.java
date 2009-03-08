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

package com.googlecode.bumblebee.samples.dao;

import java.io.Serializable;

/**
 * @author Andreas Nilsson
 * @param <P> Primary key type
 * @param <E> Entity type
 */
public interface EntityRepository<P extends Serializable, E> {

    public void save(E entity);

    public E find(P primaryKey);

    public void delete(E entity);

    public void flush();

}
