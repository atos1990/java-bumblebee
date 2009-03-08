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

package com.googlecode.bumblebee.samples.domain;

/**
 * @author Andreas Nilsson
 */
public class User {

    private Long id;

    private String username;

    private UserInfo userInfo;

    public User(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username can't be null");
        } else {
            this.username = username;
            this.userInfo = new UserInfo(this);
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof User)) {
            return false;
        } else if (o == this) {
            return true;
        } else {
            User user = (User) o;

            return username.equals(user.username);
        }
    }
}
