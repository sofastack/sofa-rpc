# Security Policy

## Reporting a Vulnerability

If you have apprehensions regarding SOFAStack's security or you discover vulnerability or potential threat, don’t hesitate to get in touch with us by dropping a mail at sofastack@antgroup.com.

In the mail, specify the description of the issue or potential threat. You are also urged to recommend the way to reproduce and replicate the issue. The SOFAStack community will get back to you after assessing and analysing the findings.

PLEASE PAY ATTENTION to report the security issue on the security email before disclosing it on public domain.

## Solution

SOFARPC uses Hessian serialization by default. Hessian is a binary serialization protocol. For more information, please refer to Hessian's [documentation](https://github.com/sofastack/sofa-hessian).

Because of the implement of Hessian, by constructing a specific serialization stream, it may cause arbitrary code execution when doing deserialization. It is recommended that users configure blacklist to solve the problem.

SOFARPC also provides a way to configure blacklists in `BlackListFileLoader`, you can override the blacklist configuration based on the code.

The blacklist built into the project comes from internal practices and external contributions, and is for reference only and is not actively updated, we do not assume any legal responsibility for this.