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
public class UserInfo {

    private Long id;

    private User user;

    private String firstName;

    private String lastName;

    public UserInfo(User user) {
        if (user == null) {
            throw new IllegalArgumentException("All parameters must be non-null");
        } else {
            this.user = user;
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode() ^ user.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof UserInfo)) {
            return false;
        } else if (o == this) {
            return true;
        } else {
            UserInfo userInfo = (UserInfo) o;

            return user.equals(userInfo.getUser());
        }
    }
}
