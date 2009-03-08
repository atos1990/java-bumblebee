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

package com.googlecode.bumblebee.samples.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import com.googlecode.bumblebee.samples.dao.UserRepository;
import com.googlecode.bumblebee.samples.domain.User;
import static com.googlecode.bumblebee.dto.Bumblebee.*;

/**
 * @author Andreas Nilsson
 */
@Service
@Transactional
public class UserServiceBean implements UserService {

    @Autowired
    private UserRepository userRepository;

    public UserData createUser(String username, String firstName, String lastName) {
        User user = new User(username);

        user.getUserInfo().setFirstName(firstName);
        user.getUserInfo().setLastName(lastName);

        userRepository.save(user);

        return assemble(UserData.class).from(user);
    }

    public UserDetails getUserDetails(String username) {
        User user = userRepository.findUserByUsername(username);

        if (user == null) {
            return null;
        } else {
            return assemble(UserDetails.class).from(user);
        }
    }
}
