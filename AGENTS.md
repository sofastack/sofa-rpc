# AGENTS.md

## Build Commands

### Build the project
```bash
mvn clean install
```

### Build without running tests
```bash
mvn clean install -DskipTests
```

### Build with specific profile
```bash
mvn clean install -Pprofile-name
```

## Lint Commands

### Run PMD checks
```bash
mvn pmd:check
```

### Run license header checks
```bash
mvn license:check
```

### Run Java formatter
```bash
mvn formatter:format
```

## Test Commands

### Run all tests
```bash
mvn test
```

### Run a specific test class
```bash
mvn test -Dtest=TestClassName
```

### Run a specific test method
```bash
mvn test -Dtest=TestClassName#testMethodName
```

### Run tests with specific profile
```bash
mvn test -Pprofile-name
```

## Code Style Guidelines

### Imports
1. Use fully qualified imports, no wildcard imports
2. Group imports in the following order:
   - Java standard library imports
   - Third-party library imports
   - Project-specific imports
3. Sort imports alphabetically within each group
4. Remove unused imports

### Formatting
1. Use 4 spaces for indentation (no tabs)
2. Line width: 120 characters
3. Align fields in classes (align_fields_grouping_blank_lines=2)
4. Keep else statement on same line as closing brace (keep_else_statement_on_same_line=false)
5. Insert space after commas in annotations and method calls
6. No space before comma in parameter lists
7. Insert space around binary operators
8. No space after prefix operators
9. Insert space after postfix operators

### Naming Conventions
1. Use camelCase for variables and methods
2. Use PascalCase for classes and interfaces
3. Use UPPER_CASE for constants
4. Use descriptive names that convey purpose
5. Avoid abbreviations except for common ones (id, url, etc.)

### Types
1. Use proper generic types when possible
2. Avoid raw types
3. Use diamond operator when possible (Java 7+)
4. Prefer interfaces over concrete classes for variable types

### Error Handling
1. Always handle exceptions appropriately
2. Don't catch exceptions and ignore them
3. Use specific exception types rather than generic Exception
4. Log exceptions with meaningful messages
5. Close resources properly (use try-with-resources when possible)

### Documentation
1. Add Javadoc to all public classes and methods
2. Keep Javadoc comments up to date
3. Use clear and concise language
4. Include @param, @return, and @throws tags as appropriate

### Additional Rules
1. Don't use System.out.println for logging (use proper logging framework)
2. Avoid magic numbers (use named constants)
3. Keep methods short and focused
4. Follow the single responsibility principle
5. Use proper thread-safe collections when needed
6. Avoid unnecessary object creation in loops
7. Use StringBuilder for string concatenation in loops
8. Prefer enhanced for loops when possible

## License Header
All files must include the Apache License 2.0 header:
```
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## PMD Rules
The project uses PMD for static code analysis. Make sure your code passes all PMD checks before submitting a pull request. Key rules include:
- CheckResultSet
- UnusedImports
- UnusedLocalVariable
- AvoidDecimalLiteralsInBigDecimalConstructor
- BrokenNullCheck
- EmptyFinallyBlock
- EmptyIfStmt
- OverrideBothEqualsAndHashcode
- ReturnFromFinallyBlock
- And many more (see tools/pmd-ruleset.xml for complete list)