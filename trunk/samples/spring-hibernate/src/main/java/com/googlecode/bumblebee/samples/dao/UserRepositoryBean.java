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

import com.googlecode.bumblebee.samples.domain.User;
import org.springframework.stereotype.Repository;

/**
 * @author Andreas Nilsson
 */
@Repository
public class UserRepositoryBean extends AbstractEntityRepository<Long, User> implements UserRepository {

    public User findUserByUsername(String username) {
        return (User) getSession().createQuery("select u from User u where u.username = :username")
                .setParameter("username", username)
                .uniqueResult();
    }

    @Override
    protected Class<Long> getPrimaryKeyClass() {
        return Long.class;
    }

    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }
}
