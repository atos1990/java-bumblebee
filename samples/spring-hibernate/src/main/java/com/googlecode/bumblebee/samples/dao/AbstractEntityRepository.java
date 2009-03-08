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

import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;

/**
 * @author Andreas Nilsson
 * @param <P> Primary key type
 * @param <E> Entity type
 */
public abstract class AbstractEntityRepository<P extends Serializable, E> implements EntityRepository<P, E> {

    @Autowired
    private SessionFactory sessionFactory;

    public void save(E entity) {
        getSession().save(entity);
    }

    @SuppressWarnings("unchecked")
    public E find(P primaryKey) {
        return (E) getSession().load(getEntityClass(), primaryKey);
    }

    public void delete(E entity) {
        getSession().delete(entity);
    }

    public void flush() {
        getSession().flush();
    }

    protected abstract Class<P> getPrimaryKeyClass();

    protected abstract Class<E> getEntityClass();

    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    protected Session getSession() {
        return sessionFactory.getCurrentSession();
    }

}
