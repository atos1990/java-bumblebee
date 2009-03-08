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

package com.googlecode.bumblebee.beans;

/**
 * @author Andreas Nilsson
 */
public class InvalidAccessorException extends RuntimeException {

    public InvalidAccessorException() {
    }

    public InvalidAccessorException(String s) {
        super(s);
    }

    public InvalidAccessorException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public InvalidAccessorException(Throwable throwable) {
        super(throwable);
    }
}
