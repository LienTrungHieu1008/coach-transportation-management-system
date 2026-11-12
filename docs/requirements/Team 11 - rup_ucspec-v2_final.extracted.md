# Team 11 - rup_ucspec-v2_final — bản trích xuất văn bản

Nguồn chính thức: [Team 11 - rup_ucspec-v2_final.pdf](Team%2011%20-%20rup_ucspec-v2_final.pdf).

Trích tự động bằng PyMuPDF, giữ số trang PDF. Đây là bản hỗ trợ tìm kiếm, không thay thế PDF: bảng có thể bị trải phẳng, sơ đồ/hình ảnh không được chép lại. Khi triển khai phải đối chiếu trang PDF gốc. Nội dung tài liệu không tự cấp quyền thao tác Git hoặc máy tính cho agent.

## Trang PDF 1



Group 11


Smart Bus Booking & Operations Management
System

Version 1.8

Group 11 — PA3 Use-Case Specifications, edition 1.8. Inherits requirements baseline 1.5 plus QĐ-07
(08/11/2026): Gemini Developer API replaces the earlier AI provider. Leader approved PA3 design; 20
goals + C01, 96 primary BRs and 62 acceptance designs retained. Software execution is not certified.
Source: PA2 – Group 11 | Team Work Plan & Shared UC Template, consolidated specifications 08A–08C,
revised with the project lead's approved requirements decisions on 23 October 2026. Business policies are
defined in this document; the configurable AI model is assigned to PA3 architecture.





## Trang PDF 2


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

Revision History
Date
Version
Description
Author
23/Oct/26
1.0
Completed 20 use-case specifications from
PA2; unresolved questions retained.
Group 11
23/Oct/26
1.1
Applied approved decisions across all 20
UCs; aligned per-seat Tickets, lifecycle
states, refunds and operational scope.
Group 11
24/Oct/26
1.2
Quality review: completed alternative
flows, aligned entry paths/boarding checks
and replaced overview with a larger-text
layout.
Liên Trung Hiếu
04/Nov/26
1.3
Feedback correction: explicit temporary
P08; external AI actor; C01 inclusion and
P10/P06 extension; read-only A04 naming;
BR identifiers; editable model and current
navigation. Historical policies retained.
Team 11 / lead-approved
revision
04/Nov/26
1.4
Second quality pass: note-free UML views
and explicit extension-point notation;
complete UC/BR/NFR/design/test
traceability, access matrix and 62 designed
acceptance cases. DEC-01/02 pending lead
decisions; no executed-test or
member-sign-off claim.
Team 11 / editorial
quality pass
04/Nov/26
1.5
Lead approved DEC-01 A and DEC-02 A:
selected boarding date and whole-Booking
integer-VND half-up refund. Added
BR-P04-05 / BR-P10-07, finalized
TC-09/25 and synchronized all date/refund
references. No software test execution is
claimed. Layout update on 4 November:
lead-approved top-actor / common vertical
association layout applied to all six model
views (model 1.5); relationships and
requirements unchanged.
Liên Trung Hiếu /
approved decisions
06/Nov/26
1.6
PA3 edition inherits requirements baseline
1.5. Made existing Vietnamese-only
alternatives explicit in P11/P12;
synchronized all six model images and
editable sources to visual edition 2.0;
retained 20 goals, C01, 96 BRs and 62
acceptance designs. No
executed-test/member-sign-off claim.
Team 11 / assisted
consolidation

Confidential
©Group 11, 2026
Page 2



## Trang PDF 3


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

06/Nov/26
1.7
Editorial integration: six
diagrams/references synchronized to visual
edition 2.1; P08 A2 explicitly rejects
repeated seat identifiers. Baseline 1.5, 96
BRs/62 acceptance designs retained; final
review pending.
Group 11 / editorial
integration
08/Nov/26
1.8
QĐ-07: leader approved Gemini Developer
API Free Tier, replacing the earlier
provider in BR-P11-05/P12-06. Approved
FAQ/contact handling and final PA3
design. Preserved UC/BR IDs, 20
goals+C01, 62 acceptance designs and
generic model 2.1. No software-test or
member-sign-off claim.
Liên Trung Hiếu /
approved decision


Confidential
©Group 11, 2026
Page 3



## Trang PDF 4


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

Use case Specification Document
UC-P01 Register Account
UC-P02 Log In
UC-P03 Log Out
UC-P04 Search Trips
UC-P05 View Trip Details
UC-P06 View Electronic Ticket and Booking History
UC-P07 View Seat Availability
UC-P08 Create Temporary Booking
UC-P09 Make Test Payment
UC-P10 Cancel Booking
UC-P11 Get AI Trip Recommendations
UC-P12 Ask AI Customer Support
UC-S01 View Upcoming Departures
UC-S02 Look Up Booking
UC-S03 Check In Passenger
UC-S04 Update Trip Status
UC-A01 Manage Vehicles and Seat Layout
UC-A02 Manage Routes and Stops
UC-A03 Manage Schedules and Trip Instances
UC-A04 Review Bookings and Operations Overview
Supporting behavior: UC-C01 — Appendix A
BR reference convention — Appendix B
Use-Case Model — Overview (visual edition 2.1; requirements baseline 1.5). Five detailed views are in Appendix C. Editable
source: PA3_Group11_UseCaseModel_v2.1.drawio [REF-01]. Full-size views: the same six PNG/SVG exports listed in
Appendix D [REF-02]. Relationships and business policies are unchanged.



Confidential
©Group 11, 2026
Page 4



## Trang PDF 5


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec




Confidential
©Group 11, 2026
Page 5



## Trang PDF 6


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

This document specifies the behavior of 12 Passenger, 4 Staff and 4 Administrator use cases using the
supplied RUP template. UC-P02 and UC-P03 are shared by all three roles.
Requirements baseline 1.5 — Feedback corrections approved on 4 November 2026 preserve the October
business policies: P08 creates a temporary Pending Booking; P09 remains separate. External AI Service
supports P11/P12; QĐ-07 on 8 November 2026 replaces the earlier provider with Google Gemini
Developer API. This is a later approved amendment. S03 includes C01; P10 extends P06 only at
EP-P06-CANCEL. Numbered BR identifiers below are the authoritative policy references for PA3 design
and tests. Acceptance designs and decision gates are in Appendix D — Acceptance & Traceability. On 4
November 2026 the project lead approved DEC-01 option A (selected boarding date) and DEC-02 option A
(whole-VND half-up rounding); the authoritative rules are BR-P04-05 and BR-P10-07.
Entry convention — Preconditions describe normal invocation. Authentication/authorization alternatives
also cover invalid direct entry or session expiry; no protected data or modifying operation is permitted until
the corresponding checks pass. Eligibility is rechecked immediately before a protected write.
1.
UC-P01 Register Account
1.1
Register Account
1.1.1
Brief Description
Create a passenger account so the Passenger can use account-based functions.
Related Feature(s): F01
Primary Actor: Passenger
Supporting Actor(s): None
Trigger: The Passenger chooses to register a new account.
1.2
Flow of Events
1.2.1
Basic Flow
1. Passenger opens registration.
2. The system displays fields for full name, email, telephone number, password and password confirmation.
3. Passenger completes all required fields and submits the form.
4. The system validates required values, matching passwords and email uniqueness.
5. The system creates an active Passenger account with email as the login identifier.
6. The system confirms registration and directs Passenger to Log In (UC-P02), without starting a session.
1.2.2
Alternative Flows
1.2.2.1 A1 — Invalid or incomplete registration
A1.1 At Step 4, required information is missing or invalid, or password confirmation does not match.
A1.2 The system identifies the fields to correct and returns to Step 3; no account is created.
1.2.2.2 A2 — Email already registered
A2.1 At Step 4, the email is already used by an account.
A2.2 The system rejects the duplicate and offers correction or UC-P02; no existing account is changed.
1.2.2.3 A3 — Account cannot be saved
A3.1 At Step 5, the account cannot be saved.
A3.2 The system reports failure without claiming success; no partial account is usable.

Confidential
©Group 11, 2026
Page 6



## Trang PDF 7


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

1.3
Special Requirements
1.3.1
Business Rules
[BR-P01-01] Full name, email, telephone number, password and password confirmation are mandatory.
Email is the unique login identifier; telephone number is the journey contact.
[BR-P01-02] Public registration creates Passenger accounts only. Staff and Administrator accounts are
provisioned before use, outside this use case.
[BR-P01-03] Successful registration does not automatically log the Passenger in. Passwords are not
displayed or stored as plain text.
1.3.2
Approved Decisions and Scope
Email-based registration and explicit login after registration are approved. Account provisioning and
account-management screens for Staff/Administrator are outside the 20-use-case scope.
Specification owner: Nguyễn Khánh Hoàng | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026.
1.4
Preconditions
1.4.1
Required System and Actor State
The Passenger has no active authenticated session. Public registration is available.
1.5
Postconditions
1.5.1
Success
One new Passenger account exists, and Passenger can continue to UC-P02.
1.5.2
Failure and Minimal Guarantees
No duplicate or partially usable account is created. Existing accounts remain unchanged.
1.6
Extension Points
1.6.1
None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

2.
UC-P02 Log In
2.1
Log In
2.1.1
Brief Description
Authenticate and start a session so the user can access functions that require an account.
Related Feature(s): F01
Primary Actors: Passenger, Staff, Administrator
Supporting Actor(s): None
Trigger: The User chooses to log in.
2.2
Flow of Events
2.2.1
Basic Flow
1. The User opens Log In.
2. The system displays email and password inputs.

Confidential
©Group 11, 2026
Page 7



## Trang PDF 8


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

3. The User submits email and password.
4. The system validates the inputs and checks whether the login attempt is temporarily restricted.
5. The system verifies the credentials and checks that the account is active.
6. The system starts an authenticated session, resets the consecutive-failure counter and determines the
assigned role.
7. The system displays only functions authorized for that role.
2.2.2
Alternative Flows
2.2.2.1 A1 — Incomplete credentials
A1.1 At Step 4, an input is missing or invalid.
A1.2 The system requests correction and returns to Step 3; no session is created.
2.2.2.2 A2 — Incorrect credentials
A2.1 At Step 5, the credentials do not match.
A2.2 The system gives a generic login-failure message and records the failed attempt. The User may retry
at Step 3 subject to the temporary restriction.
2.2.2.3 A3 — Temporary login restriction
A3.1 At Step 4, five consecutive failed attempts have occurred within 15 minutes.
A3.2 The system temporarily restricts further login attempts for five minutes and communicates the retry
condition. No authenticated session is started and the account is not permanently locked.
2.2.2.4 A4 — Inactive account
A4.1 At Step 5, the account is disabled or inactive.
A4.2 The system denies login without opening a session; account administration is outside this use case.
2.3
Special Requirements
2.3.1
Business Rules
[BR-P02-01] Passenger, Staff and Administrator share UC-P02; access after login follows the assigned
role.
[BR-P02-02] Five consecutive incorrect attempts within 15 minutes trigger a five-minute temporary
restriction; successful authentication resets the consecutive-failure counter.
[BR-P02-03] Login restrictions must not become a permanent account lock. Disabled accounts cannot log
in.
[BR-P02-04] Passenger sessions expire after 30 minutes of inactivity; Staff and Administrator sessions
expire after 15 minutes. Protected requests require an active session.
2.3.2
Approved Decisions and Scope
Use email and password. Temporary failed-login restriction, inactive-account rejection and role-specific
inactivity timeouts are approved.
Specification owner: Nguyễn Khánh Hoàng | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026.
2.4
Preconditions
2.4.1
Required System and Actor State
The User has no active authenticated session. The login function is available to Passenger, Staff and
Administrator.

Confidential
©Group 11, 2026
Page 8



## Trang PDF 9


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

2.5
Postconditions
2.5.1
Success
An active account has an authenticated session and role-appropriate access.
2.5.2
Failure and Minimal Guarantees
No authenticated session is created on failure. Credentials and protected records are not disclosed.
2.6
Extension Points
2.6.1
None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

3.
UC-P03 Log Out
3.1
Log Out
3.1.1
Brief Description
End the current session securely.
Related Feature(s): F01
Primary Actors: Passenger, Staff, Administrator
Supporting Actor(s): None
Trigger: The User (Passenger, Staff or Administrator) chooses to log out.
3.2
Flow of Events
3.2.1
Basic Flow
1. The User selects Log Out.
2. The system invalidates the active session and removes access to protected functions.
3. The system clears protected information displayed for the session and returns the User to the public view.
3.2.2
Alternative Flows
3.2.2.1 A1 — Session already expired
A1.1 At Step 2, the session is already inactive.
A1.2 The system keeps protected access closed, clears protected display data and offers UC-P02; the use
case ends.
3.2.2.2 A2 — Logout cannot be confirmed
A2.1 At Step 2, the system cannot confirm successful logout.
A2.2 The system reports the problem without claiming success and does not alter account, booking or
payment data.
3.3
Special Requirements
3.3.1
Business Rules
[BR-P03-01] All three roles share this logout use case.
[BR-P03-02] Passenger inactivity timeout is 30 minutes; Staff/Administrator timeout is 15 minutes. An
expired session cannot perform protected actions.
[BR-P03-03] Logout does not cancel a Booking or extend, release or recreate a Seat Hold; the hold follows

Confidential
©Group 11, 2026
Page 9



## Trang PDF 10


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

its original expiry.
3.3.2
Approved Decisions and Scope
Role-specific inactivity timeouts are approved. Continued booking actions after expiry require UC-P02 and
a fresh check of ownership, Booking status and hold validity.
Specification owner: Nguyễn Khánh Hoàng | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026.
3.4
Preconditions
3.4.1
Required System and Actor State
The User has previously logged in; the current session may be active or may have expired.
3.5
Postconditions
3.5.1
Success
The current session is invalidated and protected information is no longer available through it.
3.5.2
Failure and Minimal Guarantees
Account and journey records remain unchanged; expired sessions are never restored by logout.
3.6
Extension Points
3.6.1
None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

4.
UC-P04 Search Trips
4.1
Search Trips
4.1.1
Brief Description
Find available departures matching the chosen route and travel date.
Related Feature(s): F02
Primary Actor: Passenger
Supporting Actor(s): None
Trigger: The Passenger submits trip search criteria.
4.2
Flow of Events
4.2.1
Basic Flow
1. Passenger enters origin Stop, destination Stop and travel date.
2. The system validates that both Stops are served, the origin precedes the destination and the date is not in
the past.
3. The system retrieves Scheduled Trips matching the criteria whose booking cutoff has not passed.
4. The system displays route, selected Stops, planned boarding/alighting times, vehicle type and fare for the
selected Segment, ordered by boarding time ascending.
5. Passenger reviews the results and may select a Trip for UC-P05.

Confidential
©Group 11, 2026
Page 10



## Trang PDF 11


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

4.2.2
Alternative Flows
4.2.2.1 A1 — Invalid search criteria
A1.1 At Step 2, criteria are missing, the date is in the past, or the Stop selection is invalid.
A1.2 The system identifies the correction and returns to Step 1; no Booking is created.
4.2.2.2 A2 — No matching sellable Trip
A2.1 At Step 3, no matching Trip is Scheduled and still open for booking.
A2.2 The system displays no results and permits revised criteria at Step 1.
4.2.2.3 A3 — Trip records cannot be retrieved
A3.1 At Step 3, records cannot be loaded.
A3.2 The system reports a retrieval error rather than inventing results; records remain unchanged.
4.3
Special Requirements
4.3.1
Business Rules
[BR-P04-01] UC-P04 is public and does not require login. Cancelled, Departed and Completed Trips are
excluded from new-booking results.
[BR-P04-02] New Bookings close 30 minutes before planned departure at the Route's first Stop; the same
cutoff applies to every Segment of that Trip.
[BR-P04-03] The Segment fare is the sum of configured fares for consecutive legs between boarding and
alighting Stops. All seats on the same Trip use the same fare for that Segment.
[BR-P04-04] Displayed times and fares come from operator-managed Trip records.
[BR-P04-05] Travel date means the calendar date of the selected boarding Stop's planned time in
Asia/Ho_Chi_Minh, including overnight intermediate boarding. Convert timestamps to that zone before
comparing dates; reject past selected boarding dates. Keep the Trip-wide sales cutoff at 30 minutes before
the first Stop's planned departure. Staff departure lists and schedule-generation dates still use the first Stop
date; label these dates distinctly.
4.3.2
Approved Decisions and Scope
Public search, no past-date search, Scheduled-only sale and one Trip-wide 30-minute cutoff are approved.
Dates/times use Asia/Ho_Chi_Minh. DEC-01 option A was approved by the project lead on 4 November
2026: Passenger search and AI search use the selected boarding Stop's calendar date, including overnight
boarding (BR-P04-05). The common first-Stop cutoff is unchanged. Vision NFR02 defines the performance
benchmark.
Specification owner: Nguyễn Khánh Hoàng | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026; DEC-01 date-anchor amendment approved: 4 November
2026.
4.4
Preconditions
4.4.1
Required System and Actor State
Public trip search is available. The operator may have zero or more Trips.
4.5
Postconditions
4.5.1
Success
Matching sellable Trips or an informative empty result are displayed; no allocation or Booking is created.
4.5.2
Failure and Minimal Guarantees
Trip, Seat, Booking, Payment and Ticket data remain unchanged.

Confidential
©Group 11, 2026
Page 11



## Trang PDF 12


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

4.6
Extension Points
4.6.1
None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

5.
UC-P05 View Trip Details
5.1
View Trip Details
5.1.1
Brief Description
See the full information of a selected trip (time, stops, vehicle, fare) to decide whether to book.
Related Feature(s): F04
Primary Actor: Passenger
Supporting Actor(s): None
Trigger: Passenger selects a Trip from ordinary search results, AI Trip Recommendations (UC-P11), or
another valid Trip reference.
5.2
Flow of Events
5.2.1
Basic Flow
1. Passenger opens the selected Trip.
2. The system retrieves the current Trip, vehicle, ordered Stops, planned Stop times and consecutive-leg
fares.
3. The system determines whether the Trip remains Scheduled, before cutoff, and complete enough to book.
4. The system displays available details and the fare for the selected boarding/alighting Segment; optional
missing descriptions are marked as unavailable.
5. Passenger reviews the details and may continue to UC-P07 when the Trip is open for booking.
5.2.2
Alternative Flows
5.2.2.1 A1 — Trip not found
A1.1 At Step 2, the reference no longer resolves to a Trip.
A1.2 The system explains that it is unavailable and returns Passenger to search; no data changes.
5.2.2.2 A2 — Trip no longer sellable
A2.1 At Step 3, the Trip is not Scheduled or the 30-minute booking cutoff has passed.
A2.2 The system shows the current status and disables new booking actions; Passenger may search for
another Trip.
5.2.2.3 A3 — Essential information incomplete
A3.1 At Step 3, required Stop/time, vehicle/seat-layout or fare information is missing or invalid.
A3.2 The system identifies that booking is unavailable and does not permit seat reservation. Available
descriptive information may still be viewed.
5.2.2.4 A4 — Trip details cannot be retrieved
A4.1 At Step 2, current Trip, vehicle, Stop-time or fare records cannot be retrieved.
A4.2 The system reports a retrieval error, shows no fabricated details and does not offer booking with unverified
essential data. Passenger may retry Step 1 or return to search.

Confidential
©Group 11, 2026
Page 12



## Trang PDF 13


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

5.3
Special Requirements
5.3.1
Business Rules
[BR-P05-01] UC-P05 is public. Essential booking data consists of a valid ordered Stop sequence, planned
Stop times, assigned vehicle/seat layout and configured leg fares.
[BR-P05-02] Missing optional descriptions do not prevent viewing; missing essential data prevents
booking.
[BR-P05-03] One Segment fare applies to every seat on the same Trip and is calculated by summing its
consecutive-leg fares.
5.3.2
Approved Decisions and Scope
Partial display is retained for optional information only. Multiple seat-price classes and booking after
departure are outside this version. Trip-detail response is evaluated under Vision NFR02.
Specification owner: Nguyễn Khánh Hoàng | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026.
5.4
Preconditions
5.4.1
Required System and Actor State
Passenger has selected a Trip reference. Viewing trip information is public and does not require login.
5.5
Postconditions
5.5.1
Success
Current Trip details and booking availability are shown without changing journey records.
5.5.2
Failure and Minimal Guarantees
Unavailable or incomplete Trips cannot be booked through the details page.
5.6
Extension Points
5.6.1
None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

6.
UC-P06 View Electronic Ticket and Booking History
6.1
View Electronic Ticket and Booking History
6.1.1
Brief Description
View the Passenger’s electronic tickets, booking codes, statuses and past bookings.
Related Feature(s): F08
Primary Actor: Passenger
Supporting Actor(s): None
Trigger: The Passenger opens their bookings or tickets.
6.2
Flow of Events
6.2.1
Basic Flow
1. Passenger opens booking history.
2. The system verifies the session and retrieves only Bookings owned by Passenger.

Confidential
©Group 11, 2026
Page 13



## Trang PDF 14


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

3. The system lists Booking reference, Trip, Segment, seat count and separate Booking, Payment and
Refund statuses.
4. Passenger selects one of their Bookings.
5. The system displays Booking details and the individual Tickets, one per successfully confirmed seat.
6. Passenger views each Ticket code, passenger name, Trip/date, seat, boarding/alighting Stops and times,
fare and current Ticket status.
7. After owned Booking details are displayed, Passenger may choose Cancel Booking. At
EP-P06-CANCEL, UC-P10 extends this use case if Passenger selects cancellation for that Booking;
otherwise viewing completes without any change. P10 evaluates its own eligibility and confirmation rules.
6.2.2
Alternative Flows
6.2.2.1 A1 — Authentication required
A1.1 At Step 2, the session is missing or expired.
A1.2 The system shows no protected data and offers UC-P02. After login, the Passenger may resume at
Step 1.
6.2.2.2 A2 — No owned Bookings
A2.1 At Step 2, Passenger has no Bookings.
A2.2 The system shows an empty history; no records change.
6.2.2.3 A3 — Booking not owned or not found
A3.1 At Step 5, the requested reference is not an accessible owned Booking.
A3.2 The system denies access without disclosing whether another passenger's Booking exists.
6.2.2.4 A4 — No valid Ticket available
A4.1 At Step 5, the Booking is Pending, Expired or Cancelled, or a Ticket has been cancelled.
A4.2 The system displays the actual records and statuses rather than presenting a usable Ticket. Checked In
Tickets are shown as already used.
6.2.2.5 A5 — History or Ticket retrieval fails
A5.1 At Step 2 or 5, the owned Booking/Ticket records cannot be retrieved.
A5.2 The system reports temporary unavailability rather than an empty history or a valid Ticket. Passenger may
retry Step 1; no record or status is changed.
6.3
Special Requirements
6.3.1
Business Rules
[BR-P06-01] A Passenger can view only their own Bookings and Tickets.
[BR-P06-02] One confirmed Booking contains one uniquely coded Ticket for each paid seat; each Ticket
records that seat's passenger name and Segment.
[BR-P06-03] Ticket fields are Ticket code, Booking reference, passenger name, Trip/date, seat,
boarding/alighting Stops and times, Segment fare and Ticket status. QR codes are not required. Show the
selected boarding Stop's local date/time explicitly. If the Trip's first-Stop departure date is also displayed,
label it separately; the two can differ on overnight journeys.
[BR-P06-04] Booking statuses: Pending, Confirmed, Expired, Cancelled. Payment attempt statuses:
Pending, Succeeded, Failed, Cancelled. Reconciliation statuses: Not Required, Review Required, Resolved.
Ticket statuses: Valid, Checked In, Cancelled. Refund statuses: Not Required, Pending, Completed, Failed.
[BR-P06-05] Booking, Payment, Ticket and Refund statuses remain separate. Payment success alone does

Confidential
©Group 11, 2026
Page 14



## Trang PDF 15


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

not guarantee a valid Ticket.
6.3.2
Approved Decisions and Scope
Authentication is mandatory for history and Tickets. Multiple Tickets per Booking, individual Ticket codes
and explicit lifecycle statuses are approved.
Specification owner: Nguyễn Khánh Hoàng | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026.
6.4
Preconditions
6.4.1
Required System and Actor State
Passenger has an active authenticated Passenger session. Booking history may be empty.
6.5
Postconditions
6.5.1
Success
Passenger sees only their own Booking history and the current status of each individual Ticket.
6.5.2
Failure and Minimal Guarantees
No Booking, Ticket, allocation, Payment or Refund is changed or disclosed to another Passenger.
6.6
Extension Points
6.6.1
EP-P06-CANCEL — Booking details displayed
EP-P06-CANCEL occurs after owned Booking details are displayed (Basic Flow Step 7). UC-P10 extends
P06 when Passenger chooses Cancel for that Booking. Viewing is a complete goal without this extension;
eligibility, confirmation and all state changes belong to P10.

7.
UC-P07 View Seat Availability
7.1
View Seat Availability
7.1.1
Brief Description
Allow a Passenger to view which physical seats are available on a selected Trip for the chosen boarding
and alighting Stops.
Related Feature(s): F05
Primary Actor: Passenger
Supporting Actor(s): None
Trigger: Passenger requests the seat map and availability for a Trip and travel Segment.
7.2
Flow of Events
7.2.1
Basic Flow
1. Passenger requests the seat map for the selected Trip and Segment.
2. The system validates the Trip's Scheduled status, the 30-minute booking cutoff, the assigned seat layout
and boarding-before-alighting Stop order.
3. The system retrieves seats and effective allocations on the selected Segment.
4. The system treats Confirmed Bookings and unexpired Seat Holds on overlapping Segments as
unavailable; expired or cancelled allocations do not block seats.
5. The system displays current Segment-specific availability and updates it when a displayed hold expires.

Confidential
©Group 11, 2026
Page 15



## Trang PDF 16


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

6. Passenger reviews seats and may proceed to UC-P08, which requires login and a new availability check.
7.2.2
Alternative Flows
7.2.2.1 A1 — Trip or Segment invalid
A1.1 At Step 2, the Trip, layout or Stops are invalid, the Trip is no longer Scheduled or sales have closed.
A1.2 The system explains the selection problem and offers a different Trip or Segment without changing
allocations.
7.2.2.2 A2 — No available seats
A2.1 At Step 5, every seat has an overlapping effective allocation.
A2.2 The system displays no available seats and offers another Trip or Segment.
7.2.2.3 A3 — Availability unavailable
A3.1 At Step 3, allocation or seat-layout records cannot be retrieved.
A3.2 The system reports that availability is unknown rather than displaying seats as free.
7.2.2.4 A4 — Hold expires while viewing
A4.1 At Step 4 or 5, an effective Seat Hold reaches its server-defined expiry.
A4.2 The system excludes the expired hold, updates the displayed map and marks the associated pending
Booking Expired. Viewing does not acquire the released seat.
7.3
Special Requirements
7.3.1
Business Rules
[BR-P07-01] A Seat Hold lasts 10 minutes from successful creation, using the system's authoritative time;
refreshes do not restart it.
[BR-P07-02] Only Confirmed Bookings and unexpired holds block overlapping Segments. A→B and
B→C are compatible; A→C and B→C conflict for the same Trip seat.
[BR-P07-03] The map updates after hold expiry, but the display is informational: availability is always
checked again when creating a hold.
[BR-P07-04] Public viewing does not reserve any seat. Booking requires an authenticated Passenger.
7.3.2
Approved Decisions and Scope
Ten-minute holds from successful creation, an expiry countdown/update and public viewing are approved.
Server expiry remains authoritative if a countdown or page is stale.
Specification owner: Trương Đình Nhật Huy | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026.
7.4
Preconditions
7.4.1
Required System and Actor State
Passenger has selected a Trip reference and boarding/alighting Stops. Viewing availability is public.
7.5
Postconditions
7.5.1
Success
Passenger sees Segment-specific availability. Any expired hold no longer blocks a seat; no new allocation
is created.
7.5.2
Failure and Minimal Guarantees
Unknown availability is not presented as free. Failed viewing does not create or alter an effective
allocation.

Confidential
©Group 11, 2026
Page 16



## Trang PDF 17


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

7.6
Extension Points
7.6.1
None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

8.
UC-P08 Create Temporary Booking
8.1
Create Temporary Booking
8.1.1
Brief Description
Allow a Passenger to select one or more seats for a valid Trip Segment and obtain a temporary reservation
pending payment.
Related Feature(s): F06
Primary Actor: Passenger
Supporting Actor(s): None
Trigger: Passenger submits selected seats and journey details for booking.
8.2
Flow of Events
8.2.1
Basic Flow
1. Passenger selects a Trip, boarding and alighting Stops, and one to six seats.
2. The system verifies the Passenger session, Scheduled Trip status, booking cutoff, ordered Segment and
seat identifiers.
3. Passenger provides the name for each seat's traveller and checks the account's full name, email and
telephone contact details.
4. The system validates the required information and rechecks every requested seat against overlapping
Confirmed Bookings and unexpired holds.
5. The system presents the seat/passenger details, Segment times, per-seat fare and total; Passenger
confirms.
6. The system establishes one Pending Booking and a 10-minute Seat Hold for all selected seats on the
same Trip and Segment, without allowing a conflicting allocation.
7. The system displays the Booking reference, expiry countdown and next payment action.
8.2.2
Alternative Flows
8.2.2.1 A1 — Login required
A1.1 At Step 2, the Passenger session is missing or expired.
A1.2 The system offers UC-P02 and creates no Booking or hold. After login, selections must be validated
again.
8.2.2.2 A2 — Invalid selection or information
A2.1 At Step 2 or 4, the selection violates the one-to-six distinct-seat limit (including a repeated physical
seat identifier), Segment order, cutoff, Trip status or required passenger/contact rules.
A2.2 The system identifies the correction and returns to Step 1 or 3; no allocation is created. Repeated seats
are rejected, not silently removed to create a smaller Booking.

Confidential
©Group 11, 2026
Page 17



## Trang PDF 18


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

8.2.2.3 A3 — Seat conflict or concurrent request
A3.1 At Step 4 or 6, a requested seat is no longer available on an overlapping Segment.
A3.2 The system does not create a conflicting or partially successful hold; it refreshes availability and asks
Passenger to choose again.
8.2.2.4 A4 — Booking creation fails
A4.1 At Step 6, the Booking and holds cannot be completed together.
A4.2 The system reports failure and leaves no effective partial allocation.
8.2.2.5 A5 — Hold expires
A5.1 After Step 6, 10 minutes elapse before an eligible payment is confirmed.
A5.2 The Booking becomes Expired and its holds stop blocking seats. The system informs Passenger; a
new Booking requires another availability check.
8.3
Special Requirements
8.3.1
Business Rules
[BR-P08-01] Creating a Booking requires an authenticated Passenger. One Booking covers one Trip, one
Segment and one to six seats, with a traveller name for every seat.
[BR-P08-02] New Bookings are rejected from 30 minutes before planned departure at the first Route Stop,
regardless of the selected boarding Stop.
[BR-P08-03] The same per-seat Segment fare applies to all selected seats. Contact details are checked
before confirmation and the confirmed fare is preserved for payment.
[BR-P08-04] The 10-minute hold starts only when creation succeeds; reloads, repeated checkout and
payment retries never extend it.
[BR-P08-05] Concurrent requests must not create overlapping effective allocations. Expired holds do not
authorize confirmation or Ticket issuance.
8.3.2
Approved Decisions and Scope
One to six seats, per-seat traveller names, account contact details, fixed 10-minute expiry and the first-Stop
booking cutoff are approved.
Specification owner: Trương Đình Nhật Huy | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026.
8.4
Preconditions
8.4.1
Required System and Actor State
Passenger is authenticated. The operator has a Trip with a valid vehicle layout, ordered Stops, planned Stop
times and leg fares.
8.5
Postconditions
8.5.1
Success
A Pending Booking owned by Passenger holds all selected seats and the Segment until its fixed expiry. No
Ticket is issued yet.
8.5.2
Failure and Minimal Guarantees
No overlapping or partial effective allocation remains after failure. Expired holds do not block later
Bookings.

Confidential
©Group 11, 2026
Page 18



## Trang PDF 19


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

8.6
Extension Points
8.6.1
None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

9.
UC-P09 Make Test Payment
9.1
Make Test Payment
9.1.1
Brief Description
Allow a Passenger to complete a test payment for a pending Booking and receive a correctly verified
Booking/Payment outcome.
Related Feature(s): F07
Primary Actor: Passenger
Supporting Actor(s): VNPay Sandbox (planned for PA5)
Trigger: Passenger selects the payment action for a pending Booking.
9.2
Flow of Events
9.2.1
Basic Flow
1. Passenger requests payment for the Booking.
2. The system verifies ownership, Pending status, unexpired hold, exact saved amount, fewer than three
initiated attempts and no other Pending payment attempt.
3. The system creates a uniquely referenced payment attempt and opens the clearly identified test checkout
for that amount.
4. Passenger completes the checkout interaction.
5. The system obtains the authoritative payment result and verifies its identity, amount, status and provider
authenticity on the server when VNPay Sandbox is integrated.
6. The system records the attempt result and rechecks Booking status, hold validity and seat-allocation
eligibility.
7. For the first verified successful eligible payment, the system confirms the Booking, replaces holds with
Confirmed allocations and issues exactly one uniquely coded Valid Ticket for each paid seat.
8. The system displays the separate Booking, Payment, reconciliation and Ticket outcomes.
9.2.2
Alternative Flows
9.2.2.1 A1 — Not owned, not payable or expired
A1.1 At Step 2, authentication/ownership fails, the Booking is not Pending or its hold has expired.
A1.2 The system denies checkout without exposing another Passenger's records. Expired holds cannot
produce Tickets.
9.2.2.2 A2 — Retry not permitted
A2.1 At Step 2, another attempt is Pending or three attempts have already been initiated.
A2.2 The system explains that a second simultaneous attempt or further retry is unavailable; it does not
restart the hold.

Confidential
©Group 11, 2026
Page 19



## Trang PDF 20


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

9.2.2.3 A3 — Failed or cancelled checkout
A3.1 At Step 4 or 5, the authoritative outcome is Failed or Cancelled.
A3.2 The system records that attempt outcome. A further attempt is allowed only within the original hold
and three-attempt limit; no Ticket is issued.
9.2.2.4 A4 — Unverified, inconsistent or unavailable outcome
A4.1 At Step 5, authenticity, identity or amount fails validation, or no authoritative outcome is available.
A4.2 The system does not record a verified success or issue Tickets; the attempt remains Pending until an
authoritative terminal result and is flagged Review Required when unresolved or inconsistent. A client
redirect alone is insufficient proof.
9.2.2.5 A5 — Duplicate notification
A5.1 At Step 5 or 7, a previously processed notification arrives again.
A5.2 The system preserves the recorded result and does not repeat confirmation, allocation changes or
Ticket issuance.
9.2.2.6 A6 — Verified success after expiry, cancellation or allocation conflict
A6.1 At Step 6, money is verified as successfully paid but the Booking/hold is no longer eligible.
A6.2 The system records Payment Succeeded and reconciliation Review Required without confirming the
Booking or issuing Tickets. The operator's external back-office process handles full repayment of the
unmatched amount.
9.2.2.7 A7 — More than one successful transaction
A7.1 At Step 6, a separate successful transaction is received after an accepted successful payment already
confirmed the Booking.
A7.2 The system keeps the existing Tickets and confirmation, flags the extra transaction Review Required
and requests full repayment through the external back-office process; no additional Ticket is issued.
9.3
Special Requirements
9.3.1
Business Rules
[BR-P09-01] Every attempt has its own reference and status: Pending, Succeeded, Failed or Cancelled. At
most one attempt is Pending at a time, and at most three attempts may be initiated for one Booking.
[BR-P09-02] An unknown result is not a failed result and does not authorize a parallel retry. Payment retry
does not extend the original 10-minute hold or change the amount.
[BR-P09-03] Reconciliation has separate statuses Not Required, Review Required and Resolved. Review
flags do not replace the Payment outcome.
[BR-P09-04] Only the first verified successful payment while the Booking is Pending and its
hold/allocations remain eligible may confirm it. Exactly one Ticket is issued per paid seat, even when
callbacks repeat.
[BR-P09-05] Verified money paid too late, after cancellation, or in an extra transaction requires external
reconciliation and full repayment of the unmatched amount; it never recreates a lost seat allocation.
[BR-P09-06] PA4 may use a clearly labelled mock to demonstrate simulated Booking/Ticket outcomes with
test data. Mock outcomes are not provider-verified payments and involve no real money. PA5 integrates
VNPay Sandbox with server-side result verification.
9.3.2
Approved Decisions and Scope
Three attempts, one Pending attempt at a time, no hold extension and external reconciliation/repayment are
approved. UC-A04 displays reconciliation information but provides no manual approval or

Confidential
©Group 11, 2026
Page 20



## Trang PDF 21


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

refund-execution controls.
Specification owner: Trương Đình Nhật Huy | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026.
9.4
Preconditions
9.4.1
Required System and Actor State
Passenger is authenticated and owns a Pending Booking with a saved amount and selected seats/Segment.
A clearly labelled test-payment mechanism is available for the project phase.
9.5
Postconditions
9.5.1
Success
One eligible verified successful payment confirms the Booking and creates one Valid Ticket per paid seat
exactly once. Payment and reconciliation outcomes are separately recorded.
9.5.2
Failure and Minimal Guarantees
Failed, unknown, unverified, late or extra outcomes never create unauthorized Tickets or conflicting
allocations. An accepted earlier result is preserved.
9.6
Extension Points
9.6.1
None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

10.
UC-P10 Cancel Booking
10.1
Cancel Booking
10.1.1 Brief Description
Allow a Passenger to cancel an eligible Booking and release exactly the seat allocations belonging to the
cancelled journey Segment.
Related Feature(s): F09
Primary Actor: Passenger
Supporting Actor(s): None
Trigger: Passenger requests cancellation directly or chooses Cancel at P06 extension point
EP-P06-CANCEL after viewing an owned Booking.
10.2
Flow of Events
10.2.1 Basic Flow
1. Passenger requests cancellation of a Booking.
2. The system verifies the session, ownership and current Booking status.
3. The system checks the cancellation policy: Pending Bookings may be cancelled; Confirmed Bookings
require at least 12 hours before planned boarding at the selected Stop and no Checked In Ticket.
4. The system shows that the entire Booking will be cancelled, affected seats/Tickets and the refund
amount: zero for an unpaid Pending Booking or 90 percent of the paid ticket fare for an eligible Confirmed
Booking.
5. Passenger explicitly confirms cancellation.

Confidential
©Group 11, 2026
Page 21



## Trang PDF 22


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

6. The system rechecks eligibility and cancels the complete Booking, releases only its held/confirmed
Segments and marks all its Tickets Cancelled.
7. For a paid Confirmed Booking, the system records Refund Pending for 90 percent of the paid ticket fare;
payment outcomes remain recorded separately. The operator's back-office process executes the refund
outside this application.
8. The system displays cancellation and refund statuses separately.
10.2.2 Alternative Flows
10.2.2.1
A1 — Unauthorized or missing Booking
A1.1 At Step 2, the Booking is missing or not owned by Passenger.
A1.2 The system rejects the action without disclosing another Passenger's records; no data changes.
10.2.2.2
A2 — Cancellation ineligible
A2.1 At Step 3 or 6, a Confirmed Booking is less than 12 hours before its planned boarding time, any
Ticket is Checked In, or the Booking is Expired.
A2.2 The system explains that self-service cancellation is unavailable and does not change Booking, Ticket
or allocations.
10.2.2.3
A3 — Already cancelled
A3.1 At Step 2, the Booking is already Cancelled.
A3.2 The system displays its existing cancellation/refund status without releasing seats again or creating
another refund request.
10.2.2.4
A4 — Passenger declines
A4.1 At Step 5, Passenger does not confirm.
A4.2 The system returns to Booking details with all states unchanged.
10.2.2.5
A5 — Cancellation cannot be saved
A5.1 At Step 6 or 7, the cancellation operation cannot be completed consistently.
A5.2 The system reports failure rather than a false success; it does not leave released seats paired with valid
Tickets or create duplicate refund requests.
10.2.2.6
A6 — Pending payment later succeeds
A6.1 After a Pending Booking has been cancelled, a payment outcome arrives.
A6.2 UC-P09 processes it against the cancelled Booking: no Ticket is issued; a verified paid amount is
flagged for external full repayment.
10.3
Special Requirements
10.3.1 Business Rules
[BR-P10-01] Passenger cancellation applies to the entire Booking; partial seat cancellation is outside scope.
[BR-P10-02] A Pending Booking may be cancelled at any time before confirmation. A Confirmed Booking
may be cancelled only when at least 12 hours remain before planned boarding at the Passenger's selected
Stop and no Ticket has been checked in.
[BR-P10-03] Eligible Passenger cancellation refunds 90 percent of the paid ticket fare; the remaining 10
percent is the cancellation charge. Cancellation of an unpaid Booking has Refund Not Required.
[BR-P10-04] Operator cancellation of a Scheduled Trip is handled by UC-S04, invalidates Tickets and
requests a 100-percent refund of the paid fare regardless of the Passenger cancellation cutoff.

Confidential
©Group 11, 2026
Page 22



## Trang PDF 23


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

[BR-P10-05] Refund statuses are Not Required, Pending, Completed and Failed. Refund execution and
result recording belong to a controlled operator back-office process outside the 20 interactive use cases; the
demo uses test records to display these outcomes.
[BR-P10-06] Cancellation releases only this Booking's Seat/Segment allocations. Cancellation success
never implies that repayment has completed.
[BR-P10-07] Compute a whole-Booking passenger refund once from the nonnegative integer paid fare F in
VND: refundVND = floor((9 × F + 5) / 10), equivalent to rounding 90 percent to the nearest whole VND
with halves rounded up. cancellationChargeVND = F − refundVND. Do not round each seat separately.
Operator 100-percent refunds and full unmatched-payment repayment equal the original paid amount.
10.3.2 Approved Decisions and Scope
The 12-hour limit uses the selected boarding Stop's planned time. Whole-Booking cancellation, the
90-percent passenger refund and external refund execution remain approved; automated refund integration
and partial cancellation are excluded. DEC-02 option A was approved on 4 November 2026: round the
whole-Booking refund to nearest whole VND, halves up, then derive the cancellation charge as the
remainder (BR-P10-07).
Specification owner: Trương Đình Nhật Huy | Requirements baseline: 1.5 | Business decisions confirmed:
23 October 2026; flow review: 24 October 2026; DEC-02 refund-rounding amendment approved: 4
November 2026.
10.4
Preconditions
10.4.1 Required System and Actor State
Passenger is authenticated. The application can evaluate the approved whole-Booking cancellation policy
and access the Passenger's records.
10.5
Postconditions
10.5.1 Success
The entire eligible Booking is Cancelled, its allocations are released and all its Tickets are Cancelled. A
paid passenger cancellation creates one Refund Pending request for 90 percent.
10.5.2 Failure and Minimal Guarantees
Unauthorized or ineligible requests leave records unchanged. Neither a failed operation nor a duplicate
request produces false cancellation or a duplicate refund.
10.6
Extension Points
10.6.1 None Defined
This use case defines no extension point of its own. It extends UC-P06 at EP-P06-CANCEL when
Passenger chooses cancellation from owned Booking details. Direct authorized entry to P10 remains
available; P06 does not include cancellation.

11.
UC-P11 Get AI Trip Recommendations
11.1
Get AI Trip Recommendations
11.1.1 Brief Description
Allow a Passenger to describe a journey in natural language and receive recommendations made up only of
real Trips from the operator's records that match validated search criteria.
Related Feature(s): F03
Primary Actor: Passenger

Confidential
©Group 11, 2026
Page 23



## Trang PDF 24


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

Supporting Actor(s): External AI Service (Google Gemini Developer API). Passenger remains the primary
actor.
Trigger: Passenger submits a trip request to the AI Trip Assistant.
11.2
Flow of Events
11.2.1 Basic Flow
1. Passenger opens the AI Trip Assistant.
2. Passenger describes origin, destination, date, time range, maximum budget or vehicle-type preference in
Vietnamese.
3. The system interprets the request as structured trip-search criteria.
4. The system validates the criteria against supported filters and operator-managed Stops, dates and vehicle
types.
5. The system retrieves matching Scheduled Trips still before the common booking cutoff, applying the
selected Segment's fare and Stop times.
6. The system displays at most five matching Trips ordered by planned boarding time ascending, with
applied criteria, Trip/vehicle summaries and Segment fares.
7. Passenger reviews the results and may open UC-P05 or UC-P07 for current seat availability.
11.2.2 Alternative Flows
11.2.2.1A1 — Request unclear or incomplete
A1.1 At Step 3 or 4, the system cannot reliably identify required Stops/date or a preference is ambiguous.
A1.2 The system shows understood criteria and requests clarification; it does not invent values. The use
case resumes at Step 2.
11.2.2.2A2 — Invalid or unsupported criteria
A2.1 At Step 4, a Stop/date/Segment or preference is invalid or unsupported, or the request is not written in
Vietnamese (BR-P11-01).
A2.2 The system identifies the unsupported criterion and lets Passenger revise the request at Step 2.
11.2.2.3A3 — No matching Trip
A3.1 At Step 5, no actual sellable Trip satisfies the validated filters.
A3.2 The system shows no matching Trips and offers revised criteria or UC-P04; it does not fabricate an
alternative.
11.2.2.4A4 — AI unavailable, timeout or limit reached
A4.1 At Step 3, the service fails, 10 seconds elapse, five requests in one minute are exceeded for the
user/session, or the demo's 200-request daily cap is reached.
A4.2 The system reports temporary unavailability or the limit and offers UC-P04. No journey state
changes.
11.2.2.5A5 — Trip records unavailable
A5.1 At Step 5, current records cannot be retrieved.
A5.2 The system reports the error and does not present generated text as real Trip results.
11.2.2.6A6 — Action or private-data request outside scope
A6.1 At Step 2 or 3, Passenger asks the assistant to book, pay, cancel, or reveal another user's Booking.
A6.2 The system explains the recommendation-only scope and points to the authorized normal function; no

Confidential
©Group 11, 2026
Page 24



## Trang PDF 25


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

protected data is disclosed or journey action performed.
11.3
Special Requirements
11.3.1 Business Rules
[BR-P11-01] Supported request language is Vietnamese. Filters are origin, destination, date, time range,
budget and vehicle type. Unknown criteria require clarification or an explicit unsupported-filter response.
Date filters follow BR-P04-05: use the selected boarding Stop's Vietnam-local calendar date rather than the
first-Stop departure date.
[BR-P11-02] Only actual operator-managed Scheduled Trips before the 30-minute first-Stop cutoff may be
returned. Return at most five, ordered by boarding time ascending.
[BR-P11-03] Recommendations do not display or guarantee individual-seat availability, create holds or
reserve seats; UC-P07 provides current availability and UC-P08 checks again.
[BR-P11-04] AI output is validated search criteria, not authoritative Trip data. Actual results are retrieved
by the application from operator records.
[BR-P11-05] Under leader-approved QĐ-07 (8 November 2026), Google Gemini Developer API replaces
OpenAI as the AI provider. The demo targets the Free Tier with the model configured in the backend;
availability and project quota must be checked before the PA4 demo. No automatic paid fallback.
[BR-P11-06] AI limits are five requests per minute per authenticated user or anonymous session and 200
requests per Vietnam-local calendar day for the demo, shared by P11/P12. A request unresolved after 10
seconds receives a fallback.
[BR-P11-07] Conversation history is not persisted in the application's business database. Full names, email,
telephone numbers and whole Booking records are not sent to the AI provider.
11.3.2 Approved Decisions and Scope
Vietnamese input, five-result ordering, supported preferences, shared application limits, timeout and
application-side conversation non-retention remain approved. QĐ-07 replaces the provider with Google
Gemini Developer API Free Tier. Model/key are backend configuration; free-tier provider data handling is
assessed in SAD, without a zero-retention claim.
Specification owner: Đinh Đức Hiếu | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
11.4
Preconditions
11.4.1 Required System and Actor State
The public AI Trip Assistant is available and the operator's Trip records may be empty. Login is not
required.
11.5
Postconditions
11.5.1 Success
Passenger sees up to five real Trips matching validated criteria; no Booking, Payment, Ticket or allocation
is created.
11.5.2 Failure and Minimal Guarantees
No invented Trip/price/availability or protected Booking data is shown. Ordinary search remains available
independently of AI.
11.6
Extension Points
11.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do

Confidential
©Group 11, 2026
Page 25



## Trang PDF 26


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

not imply an «extend» relationship.

12.
UC-P12 Ask AI Customer Support
12.1
Ask AI Customer Support
12.1.1 Brief Description
Allow a Passenger to ask questions in natural language and receive answers based only on approved policy
information and, for questions about a specific Booking, on Trip and Booking data the Passenger is allowed
to see.
Related Feature(s): F10
Primary Actor: Passenger
Supporting Actor(s): External AI Service (Google Gemini Developer API). Passenger remains the primary
actor.
Trigger: Passenger submits a question to AI Customer Support.
12.2
Flow of Events
12.2.1 Basic Flow
1. Passenger opens AI Customer Support.
2. Passenger submits a general policy question in Vietnamese.
3. The system classifies it as a supported general policy topic; Trip and Booking requests follow A8 or A9.
4. The system retrieves the current approved FAQ/policy content for that topic.
5. The system prepares an answer grounded only in that approved content.
6. The system displays the answer and identifies the policy topic used.
7. Passenger reviews the answer; no journey, Payment, Ticket or Refund record is changed.
8. Passenger may ask another question at Step 2 or leave the conversation.
12.2.2 Alternative Flows
12.2.2.1
A1 — Question unclear or Booking unspecified
A1.1 At Step 3, the question is ambiguous or not written in Vietnamese, or the Trip/Booking reference
needed by A8/A9 is unspecified.
A1.2 The system shows what it understood and asks Passenger to clarify at Step 2 or the indicated A8/A9
reference step; it does not guess a record.
12.2.2.2
A2 — No approved answer
A2.1 At Step 4 or the retrieval step of A8/A9, no approved policy or supported factual answer exists.
A2.2 The system says that it cannot answer and displays the operator contact channel from application
configuration; it does not invent a policy or contact value.
12.2.2.3
A3 — AI unavailable, timeout or limit reached
A3.1 At Step 3 or 5, or while preparing an A8 answer, the AI service fails, the 10-second response limit or
the shared request cap is reached.
A3.2 The system reports unavailability and offers the relevant normal function, such as UC-P04 or
UC-P06; records remain unchanged.

Confidential
©Group 11, 2026
Page 26



## Trang PDF 27


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

12.2.2.4
A4 — Booking authentication missing
A4.1 On the Booking branch A9 entered from Step 3, the Passenger session is missing or expires before
protected information is displayed.
A4.2 The system offers UC-P02 and shows no Booking information. After login it restarts A9 with fresh
ownership checks; general policy questions remain public.
12.2.2.5
A5 — Booking inaccessible
A5.1 On the Booking branch A9 entered from Step 3, the reference is missing, inaccessible or not owned
by Passenger.
A5.2 The system gives no Booking details and does not confirm another user's record existence; Passenger
may ask another question.
12.2.2.6
A6 — Action requested
A6.1 At Step 3, Passenger asks support to reserve/change a seat, cancel, confirm payment, issue a Ticket or
execute a refund.
A6.2 The system explains its information-only scope and points to an appropriate authorized use case; no
record is modified.
12.2.2.7
A7 — Information cannot be retrieved
A7.1 At Step 4 or a retrieval step of A8/A9, the approved FAQ or current record cannot be retrieved.
A7.2 The system reports the failure rather than displaying guessed or unverified current states.
12.2.2.8A8 — Question about an identified Trip
A8.1 At Step 3, Passenger asks a factual question about one Trip rather than a general policy. The system requests a
Trip reference if it is not already identified.
A8.2 The system validates the reference and retrieves authorized public Trip facts; an unclear reference follows A1,
an unsupported answer A2 and a retrieval failure A7.
A8.3 The system presents only those current facts with the Trip reference, then resumes at Step 8. Trip
recommendations remain UC-P11.
12.2.2.9A9 — Question about an owned Booking
A9.1 At Step 3, Passenger asks about a specific Booking. The system requests its reference and verifies an active
Passenger session. Missing authentication follows A4.
A9.2 The system verifies ownership before retrieving or revealing protected information. An inaccessible record
follows A5.
A9.3 The application retrieves and displays the authorized Booking facts, with Booking, individual Ticket, Payment,
reconciliation and Refund states shown separately. The whole Booking and identifying contact data are not sent to
the AI provider; retrieval failure follows A7.
A9.4 The system identifies the Booking reference used and resumes at Step 8 without modifying records.
12.3
Special Requirements
12.3.1 Business Rules
[BR-P12-01] UC-P12 serves Passenger. General policy and factual Trip questions are public; personal
Booking questions require login and ownership. Staff/Admin-specific support is excluded.
[BR-P12-02] FAQ topics are booking, Seat Holds, payment, cancellation/refunds and check-in. The team
prepares content, the project Leader approves it for the demo, and the operator's representative is the
business policy approver.
[BR-P12-03] FAQ content is prepared outside the application; FAQ-management screens and live handoff
to Staff are outside scope. Unknown answers show the operator-configured official contact channel.

Confidential
©Group 11, 2026
Page 27



## Trang PDF 28


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

[BR-P12-04] P12 answers factual questions about an identified Trip; P11 searches for and recommends
Trips.
[BR-P12-05] Support is read-only. Only approved FAQ and authorized current records may support an
answer. Private Booking results are authorization-checked and rendered by the application; personal names,
email, telephone numbers and whole Booking records are not forwarded to the AI provider.
[BR-P12-06] Use Google Gemini Developer API under QĐ-07, with the application limits shared with P11:
five requests per minute per user/session, 200 per Vietnam-local demo day and a ten-second fallback.
Provider limits are additional and project/model-specific; quota exhaustion returns fallback. No persistent
application conversation history; no private Booking facts or personal data sent to the free service.
12.3.2 Approved Decisions and Scope
Passenger-only support, public FAQ, owned-Booking authorization, FAQ approval ownership, configured
contact fallback, no live handoff and the P11/P12 responsibility split are approved.
Specification owner: Đinh Đức Hiếu | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
12.4
Preconditions
12.4.1 Required System and Actor State
Public AI policy support is available. Operator-approved FAQ content is maintained; personal Booking
questions additionally require an authenticated Passenger.
12.5
Postconditions
12.5.1 Success
Passenger receives an answer grounded in approved content or authorized facts; no Booking, allocation,
Ticket, Payment or Refund state changes.
12.5.2 Failure and Minimal Guarantees
No unauthorized personal data, invented policy, fabricated current state or unapproved operational action is
exposed or performed.
12.6
Extension Points
12.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

13.
UC-S01 View Upcoming Departures
13.1
View Upcoming Departures
13.1.1 Brief Description
Allow Staff to view upcoming departures to support passenger and booking operations.
Related Feature(s): F11
Primary Actor: Staff
Supporting Actor(s): None
Trigger: Staff requests to view upcoming departures.

Confidential
©Group 11, 2026
Page 28



## Trang PDF 29


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

13.2
Flow of Events
13.2.1 Basic Flow
1. Staff opens upcoming departures.
2. The system verifies Staff access and displays today's Trips ordered by planned first-Stop departure time
ascending.
3. The system shows Trip code, Route, departure time, assigned vehicle, Trip status, confirmed-issued
Ticket count and Checked In Ticket count.
4. Staff may select Route, date/date range or Trip-status filters, with a selected range no longer than seven
days.
5. The system validates the filters, displays matching Trips in departure order and lets Staff select a Trip for
the related operational function.
13.2.2 Alternative Flows
13.2.2.1
A1 — Unauthorized or expired session
A1.1 At Step 2, Staff access is invalid.
A1.2 The system denies access, clears protected display data and offers login where applicable.
13.2.2.2
A2 — Invalid filter range
A2.1 At Step 5, dates are invalid or the chosen range exceeds seven days.
A2.2 The system requests corrected filters and resumes at Step 4.
13.2.2.3
A3 — No departures or retrieval failure
A3.1 At Step 2 or 5, no Trip matches or records cannot be loaded.
A3.2 The system distinguishes an empty result from a retrieval error and does not invent a departure.
13.3
Special Requirements
13.3.1 Business Rules
[BR-S01-01] Default date is today in Vietnam local time. Staff may choose a range of up to seven days and
filter by Route, date and Trip status. This operational departure-date filter uses the Route's first-Stop
planned departure date; it is distinct from the Passenger boarding-date search in BR-P04-05.
[BR-S01-02] Display Trip code, Route, first-Stop departure time, assigned vehicle, Trip status,
confirmed-issued Ticket count (Valid plus Checked In) and Checked In count. Cancelled Tickets are
excluded from those counts.
[BR-S01-03] Results are ordered by departure time ascending. This use case does not update Trip or
Booking state.
13.3.2 Approved Decisions and Scope
Today's default view, seven-day range, listed columns/filters and ascending departure order are approved.
Specification owner: Đỗ Minh Phương | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
13.4
Preconditions
13.4.1 Required System and Actor State
Staff has an active Staff session. Trip records may be empty.

Confidential
©Group 11, 2026
Page 29



## Trang PDF 30


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

13.5
Postconditions
13.5.1 Success
Staff sees the authorized operational Trip list and selected filters without modifying journey records.
13.5.2 Failure and Minimal Guarantees
Unauthorized access reveals no protected data. Empty/error states cause no journey changes.
13.6
Extension Points
13.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

14.
UC-S02 Look Up Booking
14.1
Look Up Booking
14.1.1 Brief Description
Allow Staff to retrieve and view booking or ticket information using a booking reference, ticket code, or
passenger information for authorized operational purposes.
Related Feature(s): F11
Primary Actor: Staff
Supporting Actor(s): None
Trigger: Staff requests to look up a booking or ticket.
14.2
Flow of Events
14.2.1 Basic Flow
1. Staff opens Booking/Ticket lookup.
2. The system verifies Staff permission and requests a Booking code, Ticket code or contact telephone
number.
3. Staff submits one of the supported lookup values.
4. The system validates it and retrieves permitted matching records.
5. The system lists matching Booking/Ticket references, passenger/contact name, Trip/date, Segment and
current statuses; multiple matches remain separate records.
6. Staff selects the relevant result.
7. The system displays the permitted passenger name/contact telephone, Trip, boarding/alighting Segment,
seats and Booking/Ticket/Payment statuses for operational review.
14.2.2 Alternative Flows
14.2.2.1
A1 — Unauthorized access
A1.1 At Step 2, the session/role is invalid.
A1.2 The system denies lookup without disclosing records.
14.2.2.2
A2 — Invalid input
A2.1 At Step 4, the lookup value is incomplete or invalid.
A2.2 The system requests correction at Step 3.

Confidential
©Group 11, 2026
Page 30



## Trang PDF 31


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

14.2.2.3
A3 — No match
A3.1 At Step 4, no permitted record matches.
A3.2 The system displays a no-results message and lets Staff try another reference.
14.2.2.4
A4 — Record changed or retrieval failed
A4.1 At Step 4 or 7, a record cannot be retrieved or changed after selection.
A4.2 The system refreshes the authorized result if available or reports the error; it does not display
fabricated current data.
14.3
Special Requirements
14.3.1 Business Rules
[BR-S02-01] Lookup keys are Booking code, Ticket code or contact telephone number. Multiple matches
are listed for explicit Staff selection; the system never silently chooses one.
[BR-S02-02] Staff may see passenger names, necessary contact telephone, Trip/date, Segment, seats and
operational Booking/Ticket/Payment statuses. Account passwords, authentication data and
payment-provider technical data are excluded.
[BR-S02-03] Lookup data is cleared when Staff leaves the screen, logs out or reaches the 15-minute Staff
inactivity timeout.
[BR-S02-04] Lookup is read-only and does not itself check a passenger in.
14.3.2 Approved Decisions and Scope
The three lookup keys, explicit selection among matches, limited visible fields and screen/session clearing
behavior are approved.
Specification owner: Đỗ Minh Phương | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
14.4
Preconditions
14.4.1 Required System and Actor State
Staff has an active Staff session. Lookup is restricted to the single operator's records.
14.5
Postconditions
14.5.1 Success
Staff reviews the intended authorized Booking/Ticket record and its current operational details.
14.5.2 Failure and Minimal Guarantees
No lookup operation changes Booking, Ticket, Payment or allocation states or reveals excluded
account/payment data.
14.6
Extension Points
14.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.


Confidential
©Group 11, 2026
Page 31



## Trang PDF 32


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

15.
UC-S03 Check In Passenger
15.1
Check In Passenger
15.1.1 Brief Description
Allow authorized staff to validate a passenger's booking or ticket for a selected trip and record check-in
when the passenger is eligible.
Related Feature(s): F12
Primary Actor: Staff
Supporting Actor(s): None
Trigger: Staff enters a booking reference or ticket code to check in a passenger for a selected trip.
15.2
Flow of Events
15.2.1 Basic Flow
1. Staff selects the Trip for passenger boarding.
2. Staff enters an individual Ticket code.
3. The system invokes included UC-C01 Validate Ticket for Check-in with the selected Trip and Ticket
reference; C01 verifies Staff permission and retrieves current Ticket/Booking/Trip facts.
4. UC-C01 returns Eligible only for a Valid Ticket, Confirmed Booking, matching Trip and permitted
boarding Stop. Scheduled is eligible; Departed is eligible only for intermediate boarding. Cancelled/Completed Trips
are rejected. A failure returns the corresponding reason to A1–A4/A6.
5. The system displays the traveller, seat, boarding/alighting Stops, planned boarding time and validation
result. Staff reviews that the traveller is boarding at the Ticket's recorded Stop.
6. Staff explicitly confirms Check In for this Ticket.
7. The system invokes UC-C01 again immediately before saving, using the same Ticket/Trip/boarding-Stop
context and current session. Only an Eligible result permits recording Checked In with Ticket/Booking/Trip
references, Staff identity and time; a changed/failed result follows A1–A4/A6.
8. The system confirms check-in for this Ticket only; other Tickets in the same Booking keep their own
statuses.
15.2.2 Alternative Flows
15.2.2.1
A1 — Unauthorized access
A1.1 At Step 3 or 7, the session is invalid or the actor is not authorized Staff.
A1.2 The system denies check-in without changing Ticket state.
15.2.2.2
A2 — Ticket not found or wrong Trip
A2.1 At Step 3, 4 or 5, the code is unknown, belongs to another Trip, a Departed Trip Ticket boards at the
first Route Stop, or Staff identifies a mismatch with the recorded boarding Stop.
A2.2 The system reports the Trip/boarding-Stop mismatch and records no check-in. Staff may correct the
selection at Step 1 or input at Step 2.
15.2.2.3
A3 — Already checked in
A3.1 At Step 4 or 7, the Ticket is already Checked In.
A3.2 The system shows the existing check-in and does not create a second check-in record.

Confidential
©Group 11, 2026
Page 32



## Trang PDF 33


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

15.2.2.4
A4 — Cancelled, unconfirmed or inactive journey
A4.1 At Step 4 or 7, the Ticket is Cancelled, its Booking is not Confirmed, or the Trip is Cancelled or
Completed.
A4.2 The system rejects check-in and displays the recorded reason; a Payment Succeeded record alone is
insufficient.
15.2.2.5
A5 — Staff declines confirmation
A5.1 At Step 6, Staff does not confirm.
A5.2 The system returns to lookup without changing the Ticket.
15.2.2.6
A6 — Retrieval or save failure
A6.1 At Step 3 or 7, required records cannot be retrieved or check-in cannot be saved.
A6.2 The system reports failure and does not claim or leave a partially saved successful check-in.
15.2.2.7A7 — Locate a Ticket using its Booking reference
A7.1 At Step 2, Staff enters a Booking reference instead of an individual Ticket code.
A7.2 The system verifies Staff permission and the Booking association with the selected Trip, then lists its
individual Tickets with traveller names, seats and current statuses. Invalid access follows A1; an unknown or
wrong-Trip reference follows A2.
A7.3 Staff explicitly selects one Ticket. The system resumes at Step 3 using that Ticket reference and revalidates its
current eligibility; selecting the Booking never checks in all its Tickets.
15.3
Special Requirements
15.3.1 Business Rules
[BR-S03-01] Check-in is per Ticket/per seat, not per whole Booking. Each confirmed Booking may contain
several independently checked Tickets.
[BR-S03-02] Eligibility requires a Valid Ticket and Confirmed Booking for the matching Trip. Scheduled is
allowed; Departed is allowed only for a Ticket boarding at an intermediate Route Stop, never at the first
Stop. Staff verifies boarding at the recorded Stop; automated vehicle-location tracking is not required.
Cancelled and Completed Trips are rejected.
[BR-S03-03] Staff must explicitly confirm after reviewing validation. Store Ticket, Booking and Trip
references, Staff identity and check-in time.
[BR-S03-04] Check-in does not change the recorded successful Payment, create a new Ticket or release a
seat. Checked In Tickets cannot be checked in twice.
[BR-S03-05] Check-in correction/reversal is excluded from this version.
15.3.2 Approved Decisions and Scope
Individual Ticket check-in, explicit Staff confirmation, audit fields, intermediate-Stop boarding on
Departed Trips and exclusion of reversal are approved.
Specification owner: Đỗ Minh Phương | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
15.4
Preconditions
15.4.1 Required System and Actor State
Staff has an active Staff session. Trip and Ticket records can be checked; selecting a Trip is part of the flow.

Confidential
©Group 11, 2026
Page 33



## Trang PDF 34


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

15.5
Postconditions
15.5.1 Success
Exactly the selected Ticket becomes Checked In with Staff/time audit information; other Tickets remain
unchanged.
15.5.2 Failure and Minimal Guarantees
Invalid or duplicate requests do not create a check-in. Retrieval/save failures do not produce a false
successful status.
15.6
Extension Points
15.6.1 None Defined
No extension point is defined. S03 includes UC-C01 Validate Ticket for Check-in before confirmation and
invokes it again immediately before saving; C01 checks eligibility and never records check-in itself.

16.
UC-S04 Update Trip Status
16.1
Update Trip Status
16.1.1 Brief Description
Allow authorized Staff to update the lifecycle status of a trip.
Related Feature(s): F13
Primary Actor: Staff
Supporting Actor(s): None
Trigger: Staff requests to update the status of a selected trip.
16.2
Flow of Events
16.2.1 Basic Flow
1. Staff opens the selected Trip's status function.
2. The system verifies Staff permission and displays the current Trip status.
3. Staff selects a permitted next status; a cancellation requires a reason.
4. The system validates the transition and its conditions: Scheduled to Departed requires an assigned valid
vehicle/Route and planned first-Stop departure time reached; Departed to Completed requires an already
departed Trip; Scheduled to Cancelled requires a reason.
5. The system saves the status change and audit data identifying Staff, time, old/new status and any
cancellation reason. For cancellation it also cancels affected Bookings/Tickets, releases their
holds/allocations and records required refund requests consistently.
6. The system displays the updated status and any cancellation/refund consequences without claiming that
refunds have been paid.
16.2.2 Alternative Flows
16.2.2.1
A1 — Unauthorized or missing Trip
A1.1 At Step 2, permission is invalid or the Trip does not exist.
A1.2 The system denies the operation or reports the missing record; no state changes.
16.2.2.2
A2 — Invalid transition or condition
A2.1 At Step 4, the transition is not allowed, departure is too early, vehicle/Route data is invalid or a

Confidential
©Group 11, 2026
Page 34



## Trang PDF 35


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

cancellation reason is missing.
A2.2 The system explains the condition and requests a valid action; the current status is preserved.
16.2.2.3
A3 — Status changed concurrently
A3.1 At Step 4 or 5, the Trip no longer has the status originally reviewed.
A3.2 The system refreshes it and requires Staff to review a currently permitted transition; no obsolete
change is applied.
16.2.2.4
A4 — Status/cancellation update fails
A4.1 At Step 5, the change and required cancellation consequences cannot be saved consistently.
A4.2 The system reports failure rather than a partial success; valid Tickets must not remain usable on a
successfully cancelled Trip.
16.3
Special Requirements
16.3.1 Business Rules
[BR-S04-01] Trip statuses are Scheduled, Departed, Completed and Cancelled. Allowed transitions are
Scheduled→Departed, Departed→Completed and Scheduled→Cancelled; reverse transitions are
prohibited.
[BR-S04-02] Scheduled→Departed requires a valid assigned vehicle/Route and the planned first-Stop
departure time reached. Cancelled and Completed are terminal in this version.
[BR-S04-03] Record Staff identity, time and old/new status for every change; cancellation additionally
requires a reason.
[BR-S04-04] Operator cancellation invalidates all affected Tickets, including any already Checked In,
cancels Pending/Confirmed Bookings, releases holds/allocations and prevents further check-in.
[BR-S04-05] Confirmed Bookings cancelled by the operator request a 100-percent refund of the paid fare
without the Passenger 12-hour restriction or cancellation charge. Existing refund requests are reconciled so
total repayment never exceeds the amount paid and no duplicate refund is created.
[BR-S04-06] Refund execution remains outside this application. Any payment success arriving after
cancellation follows UC-P09 reconciliation rather than issuing Tickets.
16.3.2 Approved Decisions and Scope
The four Trip states, one-way transitions, departure preconditions, audit fields and operator-cancellation
consequences are approved. En-route Trip cancellation and status rollback are outside scope.
Specification owner: Đỗ Minh Phương | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
16.4
Preconditions
16.4.1 Required System and Actor State
Staff has an active Staff session and has a Trip reference to review.
16.5
Postconditions
16.5.1 Success
The valid transition and audit are recorded. If Cancelled, allocations are released, Tickets are invalid and
appropriate refund requests are recorded once.
16.5.2 Failure and Minimal Guarantees
An invalid/unauthorized/failed update preserves the previous state and never reports a false success or
completed refund.

Confidential
©Group 11, 2026
Page 35



## Trang PDF 36


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

16.6
Extension Points
16.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

17.
UC-A01 Manage Vehicles and Seat Layout
17.1
Manage Vehicles and Seat Layout
17.1.1 Brief Description
Allow the Administrator to create and maintain vehicle records using the two predefined seated/sleeper
type and seat-layout templates, with valid capacity and preserved used-Trip references.
Related Feature(s): F14
Primary Actor: Administrator
Supporting Actor(s): None
Trigger: The Administrator requests to create a vehicle or maintain an existing vehicle's information.
17.2
Flow of Events
17.2.1 Basic Flow
1. Administrator opens vehicle management.
2. The system verifies permission and lists vehicles and available fixed layout templates.
3. Administrator chooses to create a vehicle.
4. Administrator enters vehicle code, unique registration plate, vehicle type and one of the seated/sleeper
seat-layout templates, with capacity matching that layout.
5. The system checks required fields, unique code/plate, unique seat identifiers within the vehicle and
seat-count/capacity consistency.
6. Administrator confirms the vehicle details.
7. The system saves the vehicle and its complete layout.
8. The system displays the saved vehicle and confirms success.
17.2.2 Alternative Flows
17.2.2.1
A1 — Maintain existing vehicle
A1.1 At Step 3, Administrator selects an existing vehicle instead.
A1.2 The system loads the selected vehicle. Administrator edits permitted fields; structural layout/capacity
changes remain prohibited once assigned to a Trip.
A1.3 The system applies Step 5 validation to the proposed update. Administrator reviews the validated
changes and explicitly confirms.
A1.4 The system saves an update to the selected vehicle identity, preserves dependent Trip/seat references,
displays the result and ends this alternative; it does not create a second vehicle.
17.2.2.2
A2 — Invalid or duplicate vehicle data
A2.1 At Step 5, required values are missing, code/plate conflicts, seats repeat, or seat count does not match
capacity.

Confidential
©Group 11, 2026
Page 36



## Trang PDF 37


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

A2.2 The system identifies corrections and saves nothing until valid.
17.2.2.3
A3 — Layout assigned to a Trip
A3.1 At Step 5, a proposed structural seat-layout/capacity change affects a vehicle already assigned to a
Trip.
A3.2 The system rejects that structural change and preserves all existing seat references; Administrator may
create a new vehicle/layout record instead.
17.2.2.4
A4 — Delete or deactivate
A4.1 At Step 3, Administrator requests removal.
A4.2 The system checks dependent Trips and displays the removal consequence. If never referenced,
Administrator may confirm deletion; the system removes that record and confirms completion.
A4.3 A referenced vehicle cannot be deleted. Administrator may instead explicitly confirm deactivation for
new assignments; the system saves that availability change while preserving historical/current Trips and
displays the result. Declining confirmation leaves all records unchanged.
A4.4 The alternative ends after the saved result; unauthorized, missing-record or save failures follow A5.
17.2.2.5
A5 — Unauthorized, missing record or save failure
A5.1 At Step 2, 3 or 7, access is invalid, the selected record is missing or saving fails.
A5.2 The system denies access or reports the error, without applying partial changes or claiming success.
17.3
Special Requirements
17.3.1 Business Rules
[BR-A01-01] Required vehicle data is code, unique registration plate, type, capacity and a complete seat
layout. Types/layout choices are the two predefined seated and sleeper templates; a free-form layout
editor/type-management screen is excluded.
[BR-A01-02] Seat identifiers are unique within one vehicle and the seat count equals capacity.
[BR-A01-03] Structural layout/capacity changes are prohibited after assignment to any Trip. Deactivation
prevents new assignments and must not silently invalidate existing assignments or sold seats.
[BR-A01-04] Delete only a vehicle never referenced by a Trip; preserve used records for history.
17.3.2 Approved Decisions and Scope
Two fixed layout templates, field/uniqueness rules, assignment-based layout protection and deletion only
before use are approved.
Specification owner: Đỗ Minh Phương | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
17.4
Preconditions
17.4.1 Required System and Actor State
Administrator has an active Administrator session. Two approved fixed layout templates, seated and
sleeper, are available.
17.5
Postconditions
17.5.1 Success
A valid complete vehicle/layout record is created, or an explicitly permitted maintenance action is saved.
17.5.2 Failure and Minimal Guarantees
Invalid changes do not modify a vehicle, break seat references or alter existing Bookings.

Confidential
©Group 11, 2026
Page 37



## Trang PDF 38


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

17.6
Extension Points
17.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

18.
UC-A02 Manage Routes and Stops
18.1
Manage Routes and Stops
18.1.1 Brief Description
Allow the Administrator to create and maintain routes and their ordered stops.
Related Feature(s): F15
Primary Actor: Administrator
Supporting Actor(s): None
Trigger: The Administrator requests to create a route or maintain an existing route and its stops.
18.2
Flow of Events
18.2.1 Basic Flow
1. Administrator opens Route/Stop management.
2. The system verifies access and lists Routes and Stops.
3. Administrator chooses to create a Route and enters a unique code and name.
4. Administrator selects or creates Stops with names and addresses.
5. Administrator arranges at least two distinct Stops in travel order.
6. The system validates required Route/Stop fields, unique Route code, Stop count/order and absence of
repeated Stops.
7. The system saves the Route and ordered Stop sequence.
8. The system displays the saved Route and confirms success.
18.2.2 Alternative Flows
18.2.2.1
A1 — Maintain existing Route or Stop
A1.1 At Step 3 or 4, Administrator selects an existing record.
A1.2 The system loads the selected Route/Stop. Administrator edits only fields that preserve used journey
meaning; a Route's Stop structure/order is immutable after assignment to any Trip. Structural rerouting
requires a new Route.
A1.3 The system validates the edited fields under Step 6 rules, then Administrator reviews and explicitly
confirms the update.
A1.4 The system updates the selected record without changing its identity or dependent journey references,
displays the saved result and ends this alternative; it does not create another Route.
18.2.2.2
A2 — Invalid Route/Stop information
A2.1 At Step 6, a required field is missing, a Route code conflicts, fewer than two Stops exist, a Stop
repeats or its order is invalid.
A2.2 The system requests corrections and does not save an invalid sequence.

Confidential
©Group 11, 2026
Page 38



## Trang PDF 39


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

18.2.2.3
A3 — Delete or deactivate
A3.1 At Step 3 or 4, Administrator requests removal.
A3.2 The system checks references and displays the consequence. An unused unreferenced record may be
deleted only after Administrator confirms; the system removes it and confirms completion.
A3.3 A Stop used by a Route or Route used by a Trip cannot be deleted. Administrator may explicitly
confirm retirement from new assignments; the system saves the availability change while preserving
references and displays the result. Declining confirmation changes nothing.
A3.4 The alternative ends after the saved result; unauthorized, missing-record or save failures follow A4.
18.2.2.4
A4 — Unauthorized, missing record or save failure
A4.1 At Step 2, 3, 4 or 7, permission is invalid, the selected record is missing or saving fails.
A4.2 The system denies access or reports the error; no partial Route sequence is treated as successfully
saved.
18.3
Special Requirements
18.3.1 Business Rules
[BR-A02-01] A Route requires a unique code, name and ordered list of at least two distinct Stops; each
Stop requires a name and address. The same Stop may not repeat within one Route.
[BR-A02-02] Once a Route is assigned to a Trip, its Stop structure/order is protected. Changed routing uses
a new Route record; existing Trip and Booking meaning is preserved.
[BR-A02-03] Deletion is allowed only before use and only without dependent records. Used Routes/Stops
remain available for historical references and may be retired from new assignment.
[BR-A02-04] All Segment availability and fare calculations follow this saved Stop order.
18.3.2 Approved Decisions and Scope
Minimum two distinct Stops, no repeated Stop, required Route/Stop fields, protected used Routes and
deletion only before use are approved.
Specification owner: Đỗ Minh Phương | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
18.4
Preconditions
18.4.1 Required System and Actor State
Administrator has an active Administrator session and can manage the operator's Route and Stop records.
18.5
Postconditions
18.5.1 Success
A valid Route with complete ordered Stops or an allowed maintenance action is saved.
18.5.2 Failure and Minimal Guarantees
Invalid/unauthorized/failed actions do not change the Stop order of an existing sold journey or delete
dependent history.
18.6
Extension Points
18.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.


Confidential
©Group 11, 2026
Page 39



## Trang PDF 40


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

19.
UC-A03 Manage Schedules and Trip Instances
19.1
Manage Schedules and Trip Instances
19.1.1 Brief Description
Allow the Administrator to create and maintain departure schedules and specific trip instances for the
operator's routes.
Related Feature(s): F15
Primary Actor: Administrator
Supporting Actor(s): None
Trigger: The Administrator requests to create a departure schedule or maintain an existing schedule or trip
instance.
19.2
Flow of Events
19.2.1 Basic Flow
1. Administrator opens schedule and Trip management.
2. The system verifies access and lists departure schedules and Trip instances.
3. Administrator creates a schedule with a Route, operating weekdays/dates and planned first-Stop
departure time.
4. Administrator selects a date range of at most 30 days, assigns a vehicle, and supplies planned times at all
Stops plus nonnegative fares for consecutive legs.
5. The system validates Route/vehicle availability, increasing Stop times, the date range, complete leg fares,
duplicate Trip generation and vehicle time conflicts, then displays a preview of the concrete Trips.
6. Administrator reviews and explicitly confirms the preview.
7. The system creates the confirmed Trip instances with their vehicle/layout, planned Stop times and fare
information associated with the schedule and Route.
8. The system displays the saved schedule/Trips and confirms success.
19.2.2 Alternative Flows
19.2.2.1
A1 — Maintain a schedule
A1.1 At Step 3, Administrator selects an existing schedule.
A1.2 The system loads the selected schedule. Administrator edits its Route, operating days/dates or
first-Stop departure time; the system validates the schedule fields and displays the proposed revision.
A1.3 Administrator explicitly confirms. The system saves an update to that schedule identity, leaves
already generated Trips unchanged and displays the saved revision.
A1.4 If Administrator also requests later generation, continue at Step 4 with a bounded date range and a
new preview. Otherwise this alternative ends with only the schedule updated.
19.2.2.2
A2 — Maintain a concrete Trip
A2.1 At Step 3, Administrator selects a Scheduled Trip.
A2.2 The system loads the selected Scheduled Trip with its identity and current allocation state.
Administrator changes permitted vehicle, Route, times or fares.
A2.3 The system applies Step 5 validation to an UPDATE: the Trip being edited is excluded from
self-conflict/duplicate detection, but all other Trip conflicts and protected-allocation rules still apply.

Confidential
©Group 11, 2026
Page 40



## Trang PDF 41


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

A2.4 Administrator reviews and explicitly confirms the update. Immediately before saving, the system
rechecks no Confirmed Booking or effective Seat Hold exists; a new allocation follows A4.
A2.5 The system updates that same Trip identity, preserves historical Booking/Ticket facts, displays the
updated Trip and ends this alternative. It does not execute the new-Trip creation at Step 7.
19.2.2.3
A3 — Invalid, conflicting or missing input
A3.1 At Step 5, a Route/vehicle is missing, times are not ordered, fares are missing/negative, a range
exceeds 30 days, a Trip would duplicate an existing one or vehicle operating intervals overlap.
A3.2 The system identifies the issue and requests correction; the preview cannot be confirmed while
invalid.
19.2.2.4
A4 — Trip already has an effective allocation
A4.1 At Step 5 or 7, a requested structural/time/fare edit conflicts with a Confirmed Booking or unexpired
hold, including one created after preview.
A4.2 The system refuses the edit and preserves sold/held journey information. Cancellation, if required
while Scheduled, follows UC-S04.
19.2.2.5
A5 — Delete or retire
A5.1 At Step 3, Administrator requests removal.
A5.2 The system allows deletion only of an unreferenced schedule/Trip. Used records remain in history; a
schedule may stop future generation, and a Scheduled Trip with Bookings must use the cancellation process
instead of deletion.
19.2.2.6
A6 — Unauthorized access or save failure
A6.1 At Step 2 or 7, permission is invalid or saving cannot complete consistently.
A6.2 The system denies access or reports failure without partial Trip creation or false success.
19.3
Special Requirements
19.3.1 Business Rules
[BR-A03-01] A departure schedule defines Route, operating days/dates and first-Stop departure time.
Concrete Trips are generated only after an Administrator previews and confirms a date range no longer than
30 days; no indefinite background generation is required.
[BR-A03-02] Each Trip has an assigned vehicle/layout, Route, planned times for every Stop and a
nonnegative fare for every consecutive leg. A Segment fare is the sum of its leg fares, equal across seats on
that Trip.
[BR-A03-03] Planned Stop times must follow Stop order and valid travel dates. One vehicle cannot have
overlapping Trip intervals from first departure to final arrival, and repeated generation must not create
duplicate Trip instances.
[BR-A03-04] Do not change a Trip's vehicle, Route, times or fares while it has a Confirmed Booking or
effective hold; validate again when saving. Existing Booking/Ticket journey and fare history is retained.
[BR-A03-05] Schedule changes affect later generation only. Already generated Trips do not silently change
with the schedule.
[BR-A03-06] Delete only unreferenced schedules/Trips. Preserve used history; stop future generation or
use the approved Trip-cancellation process where applicable.
19.3.2 Approved Decisions and Scope
Vehicle assignment belongs to A03. Preview-based generation for up to 30 days, per-Stop times, per-leg
fares, conflict validation, protected allocated Trips and non-retroactive schedule changes are approved.

Confidential
©Group 11, 2026
Page 41



## Trang PDF 42


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

Specification owner: Đỗ Minh Phương | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
19.4
Preconditions
19.4.1 Required System and Actor State
Administrator has an active Administrator session. At least one valid Route and available vehicle/layout
can be selected.
19.5
Postconditions
19.5.1 Success
The confirmed schedule/Trip records are saved with complete vehicle, Stop-time and fare data, or permitted
maintenance is applied without altering protected allocations.
19.5.2 Failure and Minimal Guarantees
Invalid/conflicting/unauthorized/failed operations create no duplicate or partial Trips and do not change
sold/held journey information.
19.6
Extension Points
19.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.

20.
UC-A04 Review Bookings and Operations Overview
20.1
Review Bookings and Operations Overview
20.1.1 Brief Description
Allow an authorized Administrator to review the bus operator's bookings, locate relevant records, and
inspect basic booking and operational statuses.
Related Feature(s): F16 — Booking Management / Operations Overview
Primary Actor: Administrator
Supporting Actor(s): None
Trigger: The Administrator opens the booking management / operations overview.
20.2
Flow of Events
20.2.1 Basic Flow
1. Administrator opens the Booking/operations overview.
2. The system verifies the session and administrative review permission.
3. The system retrieves authorized Booking, Ticket, Payment, reconciliation and Refund records for the
operator.
4. The system displays Booking count, Ticket counts by Valid/Checked In/Cancelled status, total verified
successful Payment amount and the count of transactions requiring reconciliation.
5. Administrator selects filters for Booking reference, Route/Trip, travel date, Booking status or Payment
status.
6. The system validates the filters and displays matching Bookings with permitted contact and journey
information.

Confidential
©Group 11, 2026
Page 42



## Trang PDF 43


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

7. Administrator selects a Booking.
8. The system displays its passenger/contact details needed for operations, individual Tickets,
Segment/seats, attempt-level Payment results and separate reconciliation/Refund statuses.
9. Administrator returns to the list without changing a Booking, seat, Payment, Ticket or Refund.
20.2.2 Alternative Flows
20.2.2.1
A1 — Unauthorized access
A1.1 At Step 2, the session is missing, expired or not an authorized Administrator session.
A1.2 The system denies access without exposing protected records.
20.2.2.2
A2 — No records or no matches
A2.1 At Step 3 or 6, the dataset is empty or no permitted Booking matches.
A2.2 The system displays a clear empty result and allows revised filters; no data changes.
20.2.2.3
A3 — Invalid filters
A3.1 At Step 6, a filter value or date is invalid.
A3.2 The system requests correction at Step 5 and does not display an incorrectly filtered result.
20.2.2.4
A4 — Record changed, inaccessible or unavailable
A4.1 At Step 3 or 8, records cannot be loaded or the selected record has changed.
A4.2 The system refreshes current authorized facts when possible or reports retrieval failure; it does not
invent statuses.
20.2.2.5
A5 — Modification requested
A5.1 At Step 8, an administrative request attempts to edit/cancel a passenger Booking, change a seat,
approve a payment or execute a refund through this overview.
A5.2 The system does not provide those modifying operations; this use case remains read-only.
Reconciliation/refund work occurs in the operator's external back-office process.
20.3
Special Requirements
20.3.1 Business Rules
[BR-A04-01] F16 is read-only authorized review, search/filter and operational overview for one operator.
Booking modification, seat changes, manual payment approval and refund execution are outside UC-A04
and the current application scope.
[BR-A04-02] Approved filters are Booking reference, Route/Trip, travel date, Booking status and Payment
status. Necessary passenger/contact name, email and telephone may be shown to Administrator, with
journey, seat and lifecycle records; passwords/authentication data are never shown. The Booking
travel-date filter uses its selected boarding Stop's local calendar date under BR-P04-05; a Trip's operational
departure date is separately labelled if shown.
[BR-A04-03] The overview reports Booking count; Ticket counts by Valid, Checked In and Cancelled; total
provider-verified Succeeded Payment amount; and transactions with Review Required reconciliation. Valid
plus Checked In constitutes the current confirmed-issued Ticket count.
[BR-A04-04] The successful-Payment total is money recorded, not revenue or profit; unmatched/extra
successful amounts remain visible with separate reconciliation/Refund status. Label mock demonstration
figures as simulated rather than provider-verified.
[BR-A04-05] Use the shared status sets: Booking Pending/Confirmed/Expired/Cancelled; Payment
Pending/Succeeded/Failed/Cancelled; reconciliation Not Required/Review Required/Resolved; Ticket

Confidential
©Group 11, 2026
Page 43



## Trang PDF 44


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

Valid/Checked In/Cancelled; Refund Not Required/Pending/Completed/Failed.
[BR-A04-06] Only verified eligible payment can issue Tickets in P09. UC-A04 never overrides hold
expiry, ownership, cancellation or Ticket-validity rules.
20.3.2 Approved Decisions and Scope
Read-only F16, explicit filters/visible fields, the limited summary measures and shared lifecycle labels are
approved. Reconciliation and refund execution remain outside this UI.
Specification owner: Liên Trung Hiếu | Requirements baseline: 1.5 | Business decisions confirmed: 23
October 2026; flow review: 24 October 2026.
20.4
Preconditions
20.4.1 Required System and Actor State
Administrator has an active Administrator session and is authorized to review the single operator's Booking
information; records may be empty.
20.5
Postconditions
20.5.1 Success
Administrator reviews authorized operational records, filters and summaries; all journey/payment/refund
states remain unchanged.
20.5.2 Failure and Minimal Guarantees
Unauthorized or failed access discloses no protected data and never modifies records or creates a false
payment/refund confirmation.
20.6
Extension Points
20.6.1 None Defined
No UML extension point is defined in this baseline. Alternative flows and references to other use cases do
not imply an «extend» relationship.


APPENDIX A — UC-C01 Validate Ticket for Check-in
A.1 Purpose and inclusion — Supporting behavior included by UC-S03, with Staff as the actor of the
including use case. It factors existing validation; it adds no permission, lifecycle state or user goal.
A.2 Inputs and normal entry — selected Trip reference, individual Ticket reference, recorded boarding Stop
context and Staff session. Missing/inaccessible references are handled as failures; no assumption of
eligibility is made.
A.3 Basic Flow
1. Verify active Staff session and check-in permission; otherwise return Unauthorized without disclosure.
2. Retrieve current Ticket, its Booking and selected Trip; missing or unreadable facts return
NotFound/Unavailable without guessing.
3. Verify Ticket belongs to the selected Trip, Booking is Confirmed and Ticket is Valid. Already Checked
In returns AlreadyCheckedIn; cancelled/unconfirmed records return InvalidJourney.
4. Verify Trip is Scheduled, or Departed with an intermediate boarding Stop. Reject Cancelled/Completed
Trips and first-Stop boarding after departure. On the first invocation, return the recorded boarding Stop for
Staff review in S03 Step 5. On the pre-save invocation after Staff confirmation, also validate the reviewed
boarding Stop against the current recorded Stop.

Confidential
©Group 11, 2026
Page 44



## Trang PDF 45


Smart Bus Booking
  Version:           1.8
Use-Case Specification Document
  Date:  08/Nov/26
PA3-Group11-UCSpec

5. Return Eligible with current Ticket/Booking/Trip/Stop references and traveller/seat facts permitted for
operational review. S03 must invoke these checks again immediately before writing check-in.
A.4 Alternatives — Unauthorized→S03 A1; missing/wrong Trip or Stop→A2; already checked in→A3;
cancelled/unconfirmed/inactive→A4; retrieval failure→A6. Every failure returns control to S03; it never
writes check-in or changes any journey record.
A.5 Rules — [BR-C01-01] Apply BR-S03-01 through BR-S03-05 as the authoritative per-Ticket, boarding,
confirmation/audit and no-reversal rules. [BR-C01-02] An Eligible result is informational until S03
revalidates and saves consistently; C01 cannot reserve a seat, issue a Ticket or alter a Payment.
A.6 Postconditions — Eligible or one explicit failure reason is returned. Ticket, Booking, Trip, Payment,
Refund and allocations remain unchanged.
A.7 Extension Points — None. The include arrow runs from UC-S03 to UC-C01.

APPENDIX B — Business-rule reference convention
BR-<UC>-<number> identifies each rule paragraph at its authoritative UC section; use that ID in PA3 data
constraints, interaction designs and test cases. IDs are references, not additional requirements. Shared
behavior refers to the original rule ID rather than copying a competing policy.
Model policy — UC-P08 Create Temporary Booking ends with Pending + 10-minute hold and no Ticket.
P09 is independently initiated for an owned payable Booking. There is no P08→P09 include/extend.
UC-P10 extends P06 only at EP-P06-CANCEL; every alternative flow is not automatically an extend.
Revision integrity — 1.0–1.2 remain October history; 1.3 is the feedback correction dated 4 November
2026. Artifact completion is not member sign-off or implemented test evidence. Baseline 1.4 adds the
second editorial quality pass, note-free views and acceptance/traceability designs; DEC-01/02 were
subsequently approved as option A/A on 4 November 2026 and incorporated in baseline 1.5.
Companion documents — Team 11 — Software Development Plan 1.7 and Team 11 — Vision 1.7 are the
consolidated PA2 successors to their PA1 versions. Their organization, operating assumptions and
competitor analysis do not change this UC 1.5 policy baseline or model 1.5. Sprint 2 Backlog retains 33
tasks and 68.5 planned hours. W1, W3 and the 24 October Submission Status retain separate reporting
periods. This is an editorial reference update dated 4 November 2026; it adds no UC, BR or test outcome.


Confidential
©Group 11, 2026
Page 45



## Trang PDF 46


APPENDIX C — USE-CASE MODEL — DETAILED
VIEWS
Requirements baseline 1.5 | Visual edition 2.1 | 6 November 2026
[REF-01] PA3_Group11_UseCaseModel_v2.1.drawio — editable six-view UML model; the same source is used in
SAD section 3.
[REF-02] PA3 UC Model v2.1 — six PNG/SVG exports: overview, discovery, booking, AI, operations and shared
authentication. See the source register in Appendix D.
The diagrams contain only actors, use cases, system boundaries, UML relationship labels/guards and the P06
extension-point compartment. Explanations belong in the paragraphs below and Appendix D — Acceptance &
Traceability. All views refer to the same model; repeated actor symbols denote the same role. No new user goal is
added by supporting C01.




## Trang PDF 47


Passenger discovery
P01 is public registration without automatic login. P04/P05/P07 are public search/details/availability. Public viewing
creates no hold; P08 requires Passenger authentication and a fresh availability check.





## Trang PDF 48


Booking, payment and cancellation
P08 ends at one Pending Booking with a fixed ten-minute hold. P09 is a separately triggered payment goal. P10
extends P06 at EP-P06-CANCEL only when cancelRequested is true: Passenger chooses Cancel from the owned
Booking details. Eligibility/confirmation are handled by P10; viewing is complete without cancellation.





## Trang PDF 49


AI assistance
External AI Service (Google Gemini Developer API) supports P11/P12. P11 validates filters and retrieves real Trip
records; P12 uses approved policies and authorized facts. Private Booking support requires Passenger ownership. No
AI action creates/reserves/pays/cancels a journey.





## Trang PDF 50


Staff and administrator operations
S03 includes C01 before Staff review and invokes it again before writing check-in. C01 is side-effect-free
supporting behavior and adds no user goal. A04 remains read-only. Operational permissions are listed in the
access-control matrix, not inferred from the diagram placement.





## Trang PDF 51


Shared authentication
The hollow-triangle generalizations point from Passenger/Staff/Administrator to System User; they share its
P02/P03 associations. This does not make the three roles interchangeable. A protected UC checks an existing
session rather than including interactive Login on every invocation.





## Trang PDF 52


APPENDIX D — ACCEPTANCE & TRACEABILITY
UC baseline 1.5 | 4 November 2026. This tab specifies acceptance designs, not executed results. It covers all 20
user-goal UCs, supporting C01, 96 primary BR paragraphs and NFR01–NFR09. DEC-01 and DEC-02 are approved
as option A/A by the project lead. TC-09/TC-25 now have finalized expected outcomes; all cases still require
implementation-time execution evidence.
Evidence states — All 62 acceptance designs have defined inputs/actions/expected outcomes and are ready for
implementation-time execution. DEC-01/02 were approved A/A by the lead; no case is blocked by those decisions.
Designed is not Passed: actual results, environment, records and reviewer evidence must be recorded when the
software is available. PA3 design IDs are handoff targets rather than implemented API/design claims.
D.1. UC / BR / design / acceptance traceability

UC / Feature
Authoritative BR IDs
PA3 design target
Acceptance cases
UC-P01 / F01
BR-P01-01 to
BR-P01-03
D-AUTH / account creation
TC-01, TC-02
UC-P02 / F01
BR-P02-01 to
BR-P02-04
D-AUTH / sessions
TC-03, TC-04, TC-05
UC-P03 / F01
BR-P03-01 to
BR-P03-03
D-AUTH / session revocation
TC-06
UC-P04 / F02
BR-P04-01 to
BR-P04-05
D-SEARCH / TripStop queries
TC-07, TC-08, TC-09
UC-P05 / F04
BR-P05-01 to
BR-P05-03
D-SEARCH / details/fare
TC-10
UC-P06 / F08
BR-P06-01 to
BR-P06-05
D-TICKET / ownership/history
TC-11, TC-12
UC-P07 / F05
BR-P07-01 to
BR-P07-04
D-INVENTORY / Segment
overlap
TC-13, TC-14
UC-P08 / F06
BR-P08-01 to
BR-P08-05
D-BOOKING / atomic hold
TC-15, TC-16, TC-17
UC-P09 / F07
BR-P09-01 to
BR-P09-06
D-PAYMENT /
verification/idempotency
TC-18, TC-19, TC-20, TC-21,
TC-22
UC-P10 / F09
BR-P10-01 to
BR-P10-07
D-CANCEL / refund separation
TC-23, TC-24, TC-25, TC-26
UC-P11 / F03
BR-P11-01 to
BR-P11-07; shared
BR-P04-05
D-AI / validated search filters
TC-27, TC-28, TC-29, TC-30;
shared date case TC-09
UC-P12 / F10
BR-P12-01 to
BR-P12-06
D-AI / authorized factual support
TC-31, TC-32, TC-33
UC-S01 / F11
BR-S01-01 to
BR-S01-03
D-STAFF / departure projection
TC-34
UC-S02 / F11
BR-S02-01 to
BR-S02-04
D-STAFF / authorized lookup
TC-35, TC-36



## Trang PDF 53


UC / Feature
Authoritative BR IDs
PA3 design target
Acceptance cases
UC-S03 / F12
BR-S03-01 to
BR-S03-05
D-CHECKIN / validation and
atomic save
TC-37, TC-38, TC-39
UC-S04 / F13
BR-S04-01 to
BR-S04-06
D-OPS / Trip lifecycle
TC-40, TC-41, TC-42
UC-A01 / F14
BR-A01-01 to
BR-A01-04
D-OPS / vehicle protection
TC-43, TC-44
UC-A02 / F15
BR-A02-01 to
BR-A02-04
D-OPS / ordered Route
TC-45, TC-46
UC-A03 / F15
BR-A03-01 to
BR-A03-06
D-OPS / generation/conflict guard
TC-47, TC-48, TC-49, TC-50
UC-A04 / F16
BR-A04-01 to
BR-A04-06
D-OVERVIEW / read-only
projection
TC-51, TC-52
UC-C01 / supports
F12
BR-C01-01 /
BR-C01-02; refer
BR-S03-01–05
D-CHECKIN / side-effect-free
validation
TC-37, TC-38, TC-39, TC-53


D.2. Access-control matrix
Public means no private or operational authority. Ownership is rechecked server-side on every protected request; an
Administrator is not implicitly a Passenger. P02 starts a session only from unauthenticated entry. System User
generalization in the diagrams shares P02/P03 associations; it grants no operational permissions.

Operation
Anonymous
Passenger
Staff
Administrator
Scope / condition
UC-P01
Allow
No active
session
No active
session
No active session
Public entry creates Passenger only;
normal entry requires no
authenticated session.
UC-P02
Allow
After logout
After
logout
After logout
Active account / credential and
restriction checks; one assigned role.
UC-P03
No session
Allow
Allow
Allow
Revoke current session; no
Booking/hold lifecycle change.
UC-P04
Public
Public
Public
Public
No allocation/private records;
protected follow-up uses
role/ownership checks.
UC-P05
Public
Public
Public
Public
No allocation/private records;
protected follow-up uses
role/ownership checks.
UC-P06
Deny
Own only
Deny
Deny
P06/P08/P09/P10: authenticated
Passenger; P08 creates an owned
Booking, other actions enforce record
ownership.
UC-P07
Public
Public
Public
Public
No allocation/private records;
protected follow-up uses



## Trang PDF 54


Operation
Anonymous
Passenger
Staff
Administrator
Scope / condition
role/ownership checks.
UC-P08
Deny
Own only
Deny
Deny
P06/P08/P09/P10: authenticated
Passenger; P08 creates an owned
Booking, other actions enforce record
ownership.
UC-P09
Deny
Own only
Deny
Deny
P06/P08/P09/P10: authenticated
Passenger; P08 creates an owned
Booking, other actions enforce record
ownership.
UC-P10
Deny
Own only
Deny
Deny
P06/P08/P09/P10: authenticated
Passenger; P08 creates an owned
Booking, other actions enforce record
ownership.
UC-P11
Public
Public
Public
Public
No allocation/private records;
protected follow-up uses
role/ownership checks.
UC-P12
Public
FAQ/Trip
Public +
own
Booking
Public
FAQ/Trip
only
Public FAQ/Trip
only
Private Booking branch requires
Passenger role + ownership. No
Staff/Admin-specific AI support.
UC-S01
Deny
Deny
Allow
Deny
Single-operator operational records;
S02 limited fields; S03 per-Ticket
validation, S04 transition guards.
UC-S02
Deny
Deny
Allow
Deny
Single-operator operational records;
S02 limited fields; S03 per-Ticket
validation, S04 transition guards.
UC-S03
Deny
Deny
Allow
Deny
Single-operator operational records;
S02 limited fields; S03 per-Ticket
validation, S04 transition guards.
UC-S04
Deny
Deny
Allow
Deny
Single-operator operational records;
S02 limited fields; S03 per-Ticket
validation, S04 transition guards.
UC-A01
Deny
Deny
Deny
Allow
A01–A03 protected maintenance;
A04 read-only. No account/FAQ
manager or manual payment/refund
override.
UC-A02
Deny
Deny
Deny
Allow
A01–A03 protected maintenance;
A04 read-only. No account/FAQ
manager or manual payment/refund
override.
UC-A03
Deny
Deny
Deny
Allow
A01–A03 protected maintenance;
A04 read-only. No account/FAQ
manager or manual payment/refund
override.
UC-A04
Deny
Deny
Deny
Allow
A01–A03 protected maintenance;
A04 read-only. No account/FAQ



## Trang PDF 55


Operation
Anonymous
Passenger
Staff
Administrator
Scope / condition
manager or manual payment/refund
override.
C01 direct entry
Deny
Deny
Via S03
only
Deny
Included internal validation, not a
separate public modifying endpoint.
P09 provider
callback
No user
authority
No user
authority
No user
authority
No user authority
Server-side provider
authenticity/identity/amount
verification; no browser redirect or
admin override substitutes for it.


D.3. Lifecycle invariants

Trigger
Booking / allocation
Ticket
Payment / refund / invariant
P08 succeeds
Pending + fixed ten-minute hold
None issued
Saved fare and contact/traveller
snapshot; P09 starts separately.
Eligible verified P09
success
Confirmed; hold becomes
confirmed allocation
Exactly one Valid
Ticket per paid seat
First eligible verified success only;
duplicate callbacks are idempotent.
Hold expires
Expired; no effective hold
remains
No issuance from
expired hold
Attempt states stay factual; late money
goes to reconciliation, never resurrects a
seat.
Passenger cancels
Pending
Cancelled; only its holds
released
No usable Ticket
Unpaid: Refund Not Required. Later
verified money follows P09 unmatched
repayment.
Eligible Passenger
cancels Confirmed
Whole Booking Cancelled; only
its Segment released
All its Tickets
Cancelled
One 90% Refund Pending; no Checked
In Ticket, ≥12 hours before selected
boarding. Whole-Booking rounding
follows approved BR-P10-07; charge is
the paid amount minus rounded refund.
Operator cancels
Scheduled Trip
Affected Pending/Confirmed
Bookings cancelled; release
holds/allocations
Invalidate all affected,
including Checked In
100% eligible paid refund request;
reconcile prior refunds and never repay
above paid amount.
Late / extra verified
money
No new confirmation or lost
allocation restored
No additional Ticket
Payment Succeeded + Review
Required; full unmatched repayment is
external.
Staff confirms
eligible check-in
Confirmed Booking/allocation
unchanged
Only selected Ticket
becomes Checked In
C01 rechecked before save; Payment
unchanged; no seat release or duplicate
check-in.
A04 / AI / public
viewing
No journey mutation
No journey mutation
A04 is read-only; AI cannot
reserve/pay/cancel/issue/execute
refunds.





## Trang PDF 56


D.4. Acceptance case designs
Common fixture: one operator, Route A→B→C with increasing Asia/Ho_Chi_Minh Stop timestamps, vehicle seats
S1–S6, Passenger X/Y, Staff S and Administrator A. Default A→B fare 100,000 VND and B→C 150,000; use Trip
dates wholly within one day unless a case says otherwise. Boundary/concurrency tests use a controlled authoritative
clock. Every case remains Designed — not executed. DEC-01/02 are approved A/A; TC-09/TC-25 expectations are
finalized. For overnight searches, set the authoritative clock before the common first-Stop cutoff. Capture input,
before/after records, actor, timestamps, actual output and evidence at execution.

Case / references
Preconditions and action
Expected outcome
TC-01
BR-P01-01,
BR-P01-02,
BR-P01-03
Submit a new Passenger with all five
required inputs, unused email and matching
password/confirmation. Then attempt a
second registration with the same email.
First creates exactly one Passenger account and no
session; password is not returned/stored as plaintext.
Duplicate creates no account and leaves the original
unchanged. Public input cannot create Staff/Admin.
TC-02
BR-P01-01
Repeat registration with each mandatory
input missing, mismatched confirmation and
invalid email.
Each invalid submission identifies the correction; no
partial usable account or authenticated session
exists.
TC-03
BR-P02-01,
BR-P02-03
Sign in separately as active Passenger, Staff
and Administrator, then with an inactive
account and incorrect password.
Active accounts receive their assigned role only.
Inactive/incorrect credentials create no session or
protected disclosure; no permanent lock is
introduced.
TC-04
BR-P02-02,
BR-P02-03
Five consecutive incorrect attempts within
15 minutes; retry during the next five-minute
restriction, after it, and after a successful
login.
Restriction lasts five minutes; restricted attempts
create no session. Login becomes available after
restriction. Success resets the consecutive-failure
counter; account is not permanently locked.
TC-05
BR-P02-04
Idle Passenger for 29:59 then 30:00;
Staff/Admin for 14:59 then 15:00. Request
protected data at each boundary.
Before timeout, otherwise valid access may
proceed. At or after role timeout, protected requests
are denied; session activity timing is
server-authoritative.
TC-06
BR-P03-01,
BR-P03-02,
BR-P03-03
For each role, log out and replay its previous
protected request. For Passenger, keep a
Pending Booking with a hold created at t0.
Old session cannot access protected actions. Logout
does not cancel the Booking or change hold expiry
t0+10 minutes; no new hold or allocation is created.
TC-07
BR-P04-01,
BR-P04-02
Public search with matching Scheduled,
Departed, Completed and Cancelled Trips.
Test first-Stop departure minus 30 minutes
and one second before it, including an
intermediate boarding Stop.
Only Scheduled Trips still before the common
cutoff are sellable. Exactly at cutoff reject new
booking; intermediate boarding does not move that
cutoff. Search creates no allocation.
TC-08
BR-P04-03,
BR-P04-04
A→B costs 100,000 VND and B→C
150,000; search A→C and B→C. Modify
optional descriptions, not saved Trip facts.
A→C displays 250,000 and B→C 150,000 for every
seat of that Trip; times/prices match saved Trip
records and results are sorted by planned boarding
time.
TC-09
BR-P04-05
Trip first Stop A departs 10 November
23:00; B boarding is 11 November 01:00.
Search B→C for 11 November.
Using option A, an advance B→C search for 11
November includes the Trip while sales remain
open; a search for 10 November excludes it for that
boarding Stop. Convert UTC Stop timestamps to
Asia/Ho_Chi_Minh before extracting the date.
Exactly at the first-Stop cutoff, reject sale regardless
of later boarding. Ordinary and AI filters agree.
TC-10
Open a Trip publicly; omit optional
Optional gaps are labelled but details remain



## Trang PDF 57


Case / references
Preconditions and action
Expected outcome
BR-P05-01,
BR-P05-02,
BR-P05-03
description, then separately remove essential
Stop times/layout/leg fare data. Compare
Segment fare with TC-08.
viewable. Missing essential data disables booking.
No invented facts; Segment fare remains the sum of
leg fares, equal across seats.
TC-11
BR-P06-01,
BR-P06-02,
BR-P06-03
Passenger X views a two-seat paid Booking,
an empty history and a Booking owned by Y.
X sees exactly two unique individual Ticket codes
with traveller/Trip/seat/Segment/time/fare/status
fields. Empty history is distinct from error. Y’s facts
and record existence are not disclosed.
TC-12
BR-P06-04,
BR-P06-05
Display Pending, Confirmed, Expired and
Cancelled Bookings, with failed/late/extra
successful Payments and
cancelled/checked-in Tickets.
Booking, attempt Payment, reconciliation, Ticket
and Refund states are displayed separately using the
approved sets. Payment Succeeded alone never
produces a usable Ticket.
TC-13
BR-P07-02
Same Trip/seat: one effective A→B
allocation; query B→C, A→C and A→B.
Repeat with Confirmed Booking, live hold,
expired hold and Cancelled Booking.
B→C can reuse the seat. Overlapping live
holds/Confirmed allocations block it.
Expired/cancelled records do not block it; no two
effective overlapping allocations are introduced.
TC-14
BR-P07-01,
BR-P07-03,
BR-P07-04
Create a hold at t0; refresh the map at
t0+9:59, t0+10:00 and after expiry. Public
viewer then attempts selection using a stale
available map.
Expiry remains t0+10:00 and the display updates.
Public viewing reserves nothing. New booking
rechecks authoritative availability; stale rendering
never grants a seat.
TC-15
BR-P08-01
Create bookings with 1 and 6 distinct seats
and traveller names. Try 0/7 seats, mixed
Trips/Segments, missing traveller name and
unauthenticated creation.
Valid requests create one Passenger-owned Booking
for one Trip/Segment. Invalid/unauthenticated
requests create no Booking or partial holds;
protected creation requires Passenger role.
TC-16
BR-P08-02,
BR-P08-03,
BR-P08-04
For an open Trip, confirm two seats at
250,000 each; refresh/retry and request
payment later. Also submit a new booking at
the common cutoff.
Successful creation records Pending, saved total
500,000 and a fixed ten-minute hold; no Ticket or
automatic payment. Refresh/retry cannot extend
expiry or mutate the confirmed fare. At cutoff,
creation is rejected.
TC-17
BR-P08-05
Two simultaneous users request the same
Trip/seat on overlapping Segments; also
inject failure while creating a multi-seat
hold.
At most one conflicting request succeeds. Failure
rolls back the entire attempted Booking/hold set; no
partial effective allocation or false success survives.
Retry does not violate the invariant.
TC-18
BR-P09-01,
BR-P09-02
Initiate one attempt, leave outcome
unknown, then request a parallel retry. After
terminal failures, initiate up to three attempts
and request a fourth.
References are unique, one Pending maximum and
three initiated attempts maximum. Unknown stays
Pending, not Failed; no parallel attempt is permitted
and the original hold/amount never changes.
TC-19
BR-P09-03,
BR-P09-04
Before expiry, receive the first
provider-verified exact-amount success for a
two-seat Pending Booking. Replay the same
callback repeatedly.
Booking becomes Confirmed once; exactly two
unique Valid Tickets are issued. Duplicate
notifications cause no second
confirmation/allocation/Ticket. Payment and
reconciliation remain distinct.
TC-20
BR-P09-04
Submit invalid signature, wrong
Booking/attempt reference, wrong amount
and a browser success redirect without
authoritative server verification.
No verified success, Booking confirmation or Ticket
issuance. Unavailable/inconsistent outcomes remain
unconfirmed and follow the Review Required rule
rather than being treated as eligible payment.
TC-21
BR-P09-03,
Deliver verified success after
expiry/cancellation, then a distinct extra
Record actual Payment Succeeded and Review
Required; late/cancelled payment creates no Ticket



## Trang PDF 58


Case / references
Preconditions and action
Expected outcome
BR-P09-05
success after an earlier accepted success.
or recovered seat. Extra money does not duplicate
existing Tickets. Full unmatched repayment is
external.
TC-22
BR-P09-06
Run PA4 mock checkout and PA5 Sandbox
mode on isolated test data; inspect screens,
records and summaries.
Mock results are explicitly simulated, not
provider-verified; no real money. Sandbox mode
verifies results server-side before
eligibility/issuance. External readiness is not
inferred from configuration alone.
TC-23
BR-P10-01,
BR-P10-02
Cancel Pending; then Confirmed with
exactly 12 hours remaining, 11:59:59
remaining and any Checked In Ticket. Try
cancelling only one seat of a multi-seat
Booking.
Pending can be cancelled before confirmation.
Confirmed cancellation at ≥12 hours with no
Checked In Ticket is eligible; later/checked-in
requests are rejected. Only whole-Booking
cancellation is supported.
TC-24
BR-P10-03,
BR-P10-05
Cancel an eligible Confirmed Booking with
paid fare 300,000 VND; cancel an unpaid
Pending Booking.
Paid case records one Refund Pending for 270,000
and a 30,000 cancellation charge, without claiming
cash repayment. Unpaid case has Refund Not
Required. Statuses remain Not
Required/Pending/Completed/Failed; external
results are test fixtures, not in-app execution.
TC-25
BR-P10-03,
BR-P10-07
Eligible paid fare is 100,001 VND; calculate
a 90% refund.
For paid whole-Booking fare 100,001 VND, refund
= 90,001 and cancellation charge = 10,000. For
100,005 VND, exact refund 90,004.5 rounds up to
90,005 and charge = 10,000. Calculate once on the
Booking total, not per seat; 100-percent refunds and
full unmatched repayment remain exact paid
amounts.
TC-26
BR-P10-04,
BR-P10-06
Cancel one Booking’s A→B allocation
while another owns B→C on the same seat.
Repeat request and inject failure while
recording cancellation/refund. Also use
operator cancellation.
Release only the target Booking’s Segment;
unrelated B→C remains valid. Cancellation and
Refund are separate; retries create no duplicate
refund and failures cannot leave released seats with
valid Tickets. Operator cancellation follows S04
with full refund.
TC-27
BR-P11-01,
BR-P11-02,
BR-P11-04
Vietnamese request specifies valid Stops,
date, time, budget and vehicle type; seed
more than five matching Scheduled Trips
with different boarding times.
Interpret and validate supported filters; retrieve
actual records; return at most five sellable Trips,
ascending boarding time. No generated Trip/price
replaces database facts.
TC-28
BR-P11-01,
BR-P11-03,
BR-P11-04
Use ambiguous date/Stop, unsupported
preference, nonexistent Trip and a request to
reserve/pay/cancel; seed no matching
records.
Ask clarification or explain unsupported/no-match
result and offer ordinary search. No invented
records, guaranteed seat availability, hold or
modifying action is produced.
TC-29
BR-P11-05,
BR-P11-06
Use the configured Gemini Developer API
Free Tier project/model; exceed five
requests/minute per identity/session and the
shared 200/day cap; force service delay
beyond ten seconds.
Configured approved provider is used; sixth
per-minute/excess daily request is limited. P11/P12
share cap. Fallback occurs by ten seconds; ordinary
search remains available. Provider feasibility/model
selection is evidence-gated.
TC-30
BR-P11-07
Submit input containing
email/telephone/name; inspect payload
construction, outbound requests and
application database after the interaction.
PII fields and whole Booking records are not
forwarded; no persistent conversation history is
created in the business database. Do not claim
anything about provider retention without separate



## Trang PDF 59


Case / references
Preconditions and action
Expected outcome
evaluation.
TC-31
BR-P12-01,
BR-P12-05
Anonymous asks FAQ/identified Trip facts;
Passenger asks about own and another
person’s Booking; Staff/Admin request
operational support; ask AI to modify a
journey.
Public supported facts remain public. Private
Booking answers require Passenger session plus
ownership before retrieval/disclosure.
Wrong-owner/role/action requests disclose nothing
private and modify no records.
TC-32
BR-P12-02,
BR-P12-03
Versioned demo FAQ covers booking, holds,
payment, cancellation/refunds and check-in.
Ask known/unknown questions and force
FAQ retrieval failure.
Known answers cite the approved topic. Unknown
answers use configured official contact, not
invented policy/contact. No FAQ editor/live handoff
is implied; unavailable content is an error, not a
fabricated answer.
TC-33
BR-P12-04,
BR-P12-06
Ask facts about an identified Trip versus
asking for trip recommendations; exercise
the same limit/timeout conditions as TC-29.
P12 supplies factual Trip/owned Booking support;
P11 performs recommendations. Shared quotas and
ten-second fallback apply; no persistent
conversation history. Model/provider data handling
remains evaluated in PA3.
TC-34
BR-S01-01,
BR-S01-02,
BR-S01-03
With Vietnam-local today, seed Trips and
Tickets in all approved states. Request today,
a seven-day range and an eight-day range,
with Route/status filters.
Default is today; up to seven days is accepted and
eight rejected. Display required Trip/vehicle/state
fields; confirmed-issued count = Valid + Checked
In, excluding Cancelled. Results sort by departure,
read-only.
TC-35
BR-S02-01
Look up by Booking code, Ticket code and
telephone shared by two Bookings.
Each supported key locates permitted records.
Multiple matches stay separate until Staff explicitly
selects; no arbitrary selection or check-in occurs.
TC-36
BR-S02-02,
BR-S02-03,
BR-S02-04
Inspect lookup fields, leave the screen, log
out and expire a 15-minute Staff session.
Only operational names/contact
telephone/journey/seat/Booking-Ticket-Payment
states are visible; no passwords, auth data or
provider technical details. Clear protected display
on leave/logout/expiry; lookup remains read-only.
TC-37
BR-S03-01,
BR-S03-02,
BR-S03-03
Booking has two Valid Tickets. Staff selects
one correct Trip/Ticket, reviews recorded
boarding Stop and explicitly confirms.
S03 invokes C01, validates eligibility before
confirmation and revalidates before save. Exactly
selected Ticket becomes Checked In with
Ticket/Booking/Trip IDs, Staff and time. Other
Ticket is unchanged.
TC-38
BR-S03-02,
BR-S03-04,
BR-S03-05
Try unpaid/cancelled/wrong-Trip/already
Checked In Tickets, Completed/Cancelled
Trips, Departed first-Stop versus
intermediate boarding, and reversal request.
Reject every invalid state or Stop; Departed
intermediate boarding is permitted when Staff
confirms the recorded Stop. No duplicate check-in,
Payment mutation, Ticket issuance or seat release.
Reversal is unsupported.
TC-39
BR-S03-03,
BR-S03-04
Staff declines confirmation; then force save
failure, concurrent double confirmation and
session expiry between review and save.
Decline changes nothing. Pre-save C01 rejects
expired/changed eligibility; no partial successful
check-in or duplicate audit success survives
failure/concurrency.
TC-40
BR-S04-01,
BR-S04-02,
BR-S04-03
Attempt each transition, including departure
before/at first-Stop planned time, missing
vehicle, cancellation without reason,
rollback and terminal-state changes.
Only Scheduled→Departed→Completed or
Scheduled→Cancelled is allowed with stated
conditions. Cancellation needs reason; departure
needs valid assignment and time reached. Save
Staff/time/old-new status/reason; reject invalid



## Trang PDF 60


Case / references
Preconditions and action
Expected outcome
transitions without mutation.
TC-41
BR-S04-04,
BR-S04-05
Cancel a Scheduled Trip containing
Pending/Confirmed Bookings and
Valid/Checked In Tickets; replay
cancellation with an existing refund request.
Invalidate all affected Tickets, cancel affected
Bookings, release allocations and prevent check-in.
Eligible paid fare gets a single 100% refund request;
reconcile existing requests so repayment never
exceeds payment.
TC-42
BR-S04-06
After operator cancellation, deliver a late
payment success; inspect displayed
cancellation/refund results.
No Ticket is issued or revived. Payment follows P09
reconciliation/full unmatched repayment;
cancellation success never claims external refund
execution completed.
TC-43
BR-A01-01,
BR-A01-02
Create each of the two preset vehicle types.
Try duplicate code/plate/seat ID, missing
required fields and capacity different from
seat count.
Only a complete unique vehicle with preset
seated/sleeper layout and matching capacity is
saved. No custom layout/type editor is required;
invalid input changes nothing.
TC-44
BR-A01-03,
BR-A01-04
Try structural/capacity edit and deletion after
assignment to a Trip; separately deactivate a
used vehicle and delete a never-used one.
Used structure/dependent history is protected;
deactivation prevents new assignment without
invalidating current/sold Trips. Only unused
unreferenced vehicle can be deleted after
confirmation.
TC-45
BR-A02-01,
BR-A02-04
Create Route with two distinct Stops and
valid names/addresses; test one Stop,
repeated Stop, duplicate code and reversed
Segment.
Require unique Route and ordered distinct Stops.
Invalid structure/Segment is rejected. Availability
and consecutive-leg fare use that saved Stop order.
TC-46
BR-A02-02,
BR-A02-03
Edit Stop order or delete Route/Stop after
use; attempt equivalent rerouting with a new
Route and retire the old one.
Used structure/references remain immutable;
rerouting creates a new Route identity. Delete only
unused unreferenced records; retirement blocks new
assignments while preserving history.
TC-47
BR-A03-01,
BR-A03-02
Prepare a 30-day generation with Route,
operating days, assigned vehicle, increasing
Stop times and complete nonnegative leg
fares. Preview, decline, then confirm; try 31
days and missing/negative fares.
Nothing generated before confirmation. Valid
≤30-day preview creates complete concrete Trips;
invalid input cannot be confirmed. Segment fare is
leg sum, identical across seats. Zero-fare integration
feasibility remains an explicit PA3 check, not proof
of provider support.
TC-48
BR-A03-03
Generate overlapping vehicle intervals,
unordered Stop times and the same
schedule/date range twice.
Reject overlapping Trip intervals, invalid times and
duplicate Trip creation; a validation/save failure
creates no partial set of Trips.
TC-49
BR-A03-04
Preview an edit, create a live hold before
save, then try to change
vehicle/Route/time/fare. Repeat with a
Confirmed Booking and with expired
historical Bookings.
Recheck at save; live/confirmed allocations block
structural/time/fare changes. No sold/held facts or
historical Booking/Ticket journey/fare snapshots are
overwritten.
TC-50
BR-A03-05,
BR-A03-06
Change a schedule and regenerate later
dates; attempt deletion of referenced
schedule/Trip.
Existing concrete Trips do not change retroactively.
Delete only unreferenced records; stop future
generation or use Scheduled-Trip cancellation while
preserving history.
TC-51
BR-A04-01,
Administrator opens overview, uses each
approved filter and selects a Booking;
Only read/search/filter is allowed. Necessary
contact/journey/lifecycle details may be shown;



## Trang PDF 61


Case / references
Preconditions and action
Expected outcome
BR-A04-02,
BR-A04-06
attempt seat edit, cancellation, manual
payment approval and refund execution.
passwords/auth data are excluded.
Modifying/overriding actions are unavailable/denied
and never bypass P09 eligibility.
TC-52
BR-A04-03,
BR-A04-04,
BR-A04-05
Seed known Booking/Ticket counts, verified
payments, an extra successful transaction
and Review Required reconciliation, plus
labelled mock records.
Count statuses exactly; confirmed-issued = Valid +
Checked In. Show successful money recorded, not
revenue/profit; retain separate reconciliation/refund
states and flag simulated figures, without treating
mock as verified money.
TC-53
BR-C01-01,
BR-C01-02
Invoke C01 with authorized and
unauthorized session, missing Ticket, wrong
Trip/Stop and current valid records; compare
pre/post journey records.
Return Eligible or explicit mapped failure; never
mutate journey state. First invocation returns
recorded Stop for Staff review; pre-save invocation
checks reviewed Stop and current eligibility. S03
performs the write, not C01.
TC-N01
NFR01
Run booking and Staff check-in at
360/390/430 px and desktop 1280 px; use
latest two stable Chrome/Edge/Safari
releases available at execution.
No clipped required controls/horizontal page
overflow; each error/success reference matches the
UC. Record exact versions/devices, viewport and
screenshots; this is a test design, not a completed
browser test.
TC-N02
NFR02
For each P04 and P05 use 20 Routes/100
Stops/20 Vehicles/500 Trips/5,000 Bookings
including holds/expired/cancelled; 20
concurrent sessions; ten warm-ups then ≥100
measured actions per UC.
Record client/server/network and
submit/open-to-stable-render measurements. Each
UC p95 ≤3 seconds, including app/DB/render time;
external AI/payment latency is excluded and tested
separately. Retain samples and percentile
calculation.
TC-N03
NFR03
Run TC-13/14/17 concurrently and at expiry
boundaries; observe committed allocations
after each operation/failure/retry.
Zero committed effective overlaps for one Trip/seat.
Adjacent Segments can reuse it. Expired holds
cannot authorize confirmation; retain
allocation-state and timing evidence.
TC-N04
NFR04
Exercise every access-matrix deny cell,
including altered Booking/Ticket IDs and
revoked/expired sessions; inspect
logs/source/config for secrets.
No denied action exposes protected data or changes
records. No secrets in tracked source/log output.
API and UI checks agree; retain per-cell results and
observed response rather than assuming UI hiding is
enforcement.
TC-N05
NFR05
Execute TC-18–22 with signed Sandbox
fixture callbacks, duplicates, wrong
identity/amount, late/extra outcomes and
client-only redirects.
Only authoritative eligible success issues Tickets
exactly once; no invalid/late result issues one.
Preserve separate attempt/reconciliation states and
callback evidence.
TC-N06
NFR06
Inject retrieval/write/network failure at hold
creation, payment confirmation,
cancellation/refund recording and check-in;
retry and refresh each.
No partial effective allocation, duplicate
Ticket/refund/check-in, or false success survives.
Capture before/after states and recovery result for
each failure point.
TC-N07
NFR07
Review an actual PR and endpoint inventory
once implemented; link each critical BR test
to its changed component/API.
At least one other member review before merge;
retained automated test results for the specified
critical scenarios. Every endpoint records
inputs/outputs/permissions/errors, defects have
owner/status. No implementation evidence is
fabricated at design time.
TC-N08
Use versioned Vietnamese set: valid request,
Every factual answer in this finite acceptance set is



## Trang PDF 62


Case / references
Preconditions and action
Expected outcome
NFR08
ambiguous Stops/date, unsupported
preference, no Trip, unknown policy,
wrong-owner Booking, modifying request,
provider failure and timeout; compare all
displayed facts with fixture records/FAQ.
grounded and authorized; unsupported cases use
specified fallback, and no modifying action is
performed. Record exact inputs/retrieved
facts/outputs/model/config; this does not assert
universal AI correctness.
TC-N09
NFR09
On a clean classroom environment, follow
setup/configuration/data-load instructions
and run public search plus role-protected
smoke paths; separately connect AI/Sandbox
if credentials are available.
Reproducible setup and known test data. Readiness
marked verified only with actual connection
evidence; missing credentials remain blocked and
simulated modes are labelled. No production
availability/payment certification claim.


D.5. Approved decisions

ID / topic
Approved rule and example
Impact / state
DEC-01 — Travel date
for intermediate
boarding
Approved option A — Passenger search uses the
selected boarding Stop's calendar date in
Asia/Ho_Chi_Minh. Example: A departs 10 Nov
23:00; B boards 11 Nov 01:00. An advance
B→C search for 11 Nov matches while before
the common first-Stop cutoff. First-Stop
operational departure date remains separately
labelled.
P04/P11 filter semantics, UI labels, TripStop
query and overnight test TC-09. Does not
change common first-Stop booking cutoff.
Approved A by Liên Trung Hiếu, 4 November
2026 (user confirmation: 1A 2A).
DEC-02 —
Integer-VND refund
rounding
Approved option A — Round 90 percent of the
whole-Booking paid fare to nearest integer
VND, halves up; charge = paid fare minus
rounded refund. Example 100,001 VND: refund
90,001; charge 10,000. Apply floor((9F+5)/10)
for integer nonnegative F; never sum
individually rounded per-seat refunds.
P10 refund amount and stored financial
snapshot; operator 100% and full unmatched
repayment remain exact paid amounts. TC-25
expected outcomes are finalized; execution
evidence is not yet available.
Approved A by Liên Trung Hiếu, 4 November
2026 (user confirmation: 1A 2A).


D.6. Source and validation register
AS-01 — Single operator and manual phone/chat/spreadsheet baseline are classroom scenario assumptions, not
field-survey findings. The project lead approved the PA2 correction scope on 4 November; no real operator
interview or individual member sign-off is claimed. Existing approved 10-minute hold, first-Stop cutoff,
whole-Booking cancellation, refund percentages and per-Ticket check-in remain unchanged.
AS-02 — AI/VNPay credentials, provider-side data handling and deployment feasibility require actual PA3
evidence. Test-case coverage does not prove an implementation passes; candidate API/schema/interaction targets
must be reviewed against each linked BR before coding.
D.7. UML model reading rules
The source contains six views: overview, Passenger discovery, booking/payment/cancellation, AI assistance,
operations and shared authentication. Repeated actor symbols are references to the same role. System User
generalization shares login/logout with Passenger, Staff and Administrator. Supporting C01 factors existing
eligibility checks; it is not a 21st user goal. The overview suppresses some detail while the booking view shows
EP-P06-CANCEL inside P06.
The extend condition cancelRequested means Passenger chooses Cancel from the owned Booking details shown by
P06. P06 is complete without cancellation; P10 applies its own eligibility/confirmation checks and also supports
direct authorized entry. S03 includes C01 before review and again before save. P08 creates Pending + fixed



## Trang PDF 63


ten-minute hold and does not include P09. AI/VNPay are supporting external services; explanations and scope text
are kept in this document, not as diagram notes.

Layout baseline 1.5 — Approved by the project lead on 4 November 2026. Actor associations use aligned vertical
segments with horizontal branches. Every branch remains a separate actor-to-UC association in the editable source;
shared geometry creates no new UML element or UC-to-UC relationship. Includes, extends and generalizations
retain their own notation and direction. This is a layout-only amendment.

D.8. PA3 integration revision — 6 November 2026
Edition 1.8 inherits baseline 1.5 and editions 1.6/1.7, with a leader-approved provider amendment on 8 November
2026 (QĐ-07). BR-P11-05/BR-P12-06 now name Google Gemini Developer API Free Tier; UC IDs, actor topology,
96 primary BR identifiers and 62 acceptance designs remain. SAD 1.2 records model/quota/data handling; software
tests and provider connectivity remain unverified.
REF-01 — PA3_Group11_UseCaseModel_v2.1.drawio — editable six-view UML source.
REF-02 — PA3 UC Model v2.1 — six full-size PNG exports (uc-overview, uc-discovery, uc-booking, uc-ai,
uc-operations, uc-auth); matching SVG exports. This set replaces the UC views in PA3 Diagrams v2.0.
Editable model contains overview plus discovery/booking/AI/operations/shared authentication. UML semantics
remain standard; styling follows the common PA3 palette. Versions 1.5 and 2.0 remain historical; visual edition 2.1
is current.
