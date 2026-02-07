# blocking-vs-nonblocking-io



This repository explores the difference between blocking socket-based I/O and

selector-driven non-blocking I/O through two implementations of a group

broadcast server in Java.



The goal of this project is to understand how different I/O models affect

control flow, scalability, and program structure.



\## Structure



\- `NetworkingIO/`  

&nbsp; Blocking implementation using traditional sockets.  

&nbsp; Typically follows a thread-per-connection model.



\- `NetworkingNIO/`  

&nbsp; Non-blocking implementation using Java NIO and selectors.  

&nbsp; Uses readiness-based event handling and explicit connection state.



\## Features



\- Group message broadcasting

\- Multiple client connections

\- Comparison of blocking vs non-blocking designs



\## Current Limitations



\- Broadcast-only messaging (no private channels)

\- Simple message framing

\- No authentication or persistence



\## Motivation



This project was built to move beyond high-level abstractions and understand

networking at the I/O and event-loop level.



\## Future Improvements



\- Private messaging

\- Better protocol framing

\- Backpressure handling

\- Cleaner separation of connection state



