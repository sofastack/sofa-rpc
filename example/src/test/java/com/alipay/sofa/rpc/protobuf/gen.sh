#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# CHANGE USER_DIR to your project address
#export USER_DIR="~/workspace/github.com/alipay/sofa-rpc"
export USER_DIR="/Users/zhanggeng/workspace/github.com/ujjboy/sofa-rpc-stable"
export PROTO_PATH="${USER_DIR}/example/src/test/java/com/alipay/sofa/rpc/protobuf"
export OUTPUT_PATH="${USER_DIR}/example/src/test/java"

# protoc need add to $PATH
protoc \
    --java_out=${OUTPUT_PATH} \
    --proto_path=${PROTO_PATH} \
    ${PROTO_PATH}/ProtoService.proto