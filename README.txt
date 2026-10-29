ONIVISTA AGRO - TEACHER READY JAVA WEBSITE
===========================================

REQUIREMENT
-----------
JDK 17 or newer is required. Node.js is NOT required.

HOW TO START
------------
1. Extract this ZIP to a normal folder, e.g. Desktop.
2. Open the onivista_java folder.
3. Double-click start.bat.
4. Keep the black Java window open.
5. Chrome will open http://localhost:3000 automatically.

IMPORTANT
---------
Do NOT double-click index.html.
The website must be opened through http://localhost:3000 because the
Java server provides the dynamic review API.

ADMIN
-----
URL: http://localhost:3000/admin.html
Password: onivista123

REVIEW DEMO
-----------
1. Open homepage.
2. Scroll to Customer Reviews.
3. Enter name, choose stars, write review and submit.
4. Review is stored as Pending in data/reviews.json.
5. Open Admin page and log in.
6. Click Approve.
7. Return to homepage and refresh.
8. The approved review appears publicly.

PROJECT TECHNOLOGY
------------------
Frontend: HTML5, CSS3, JavaScript
Backend: Java HttpServer (JDK 17+)
Storage: JSON file (data/reviews.json)

TROUBLESHOOTING
---------------
If the page says "Unable to connect to the Java server":
- Close any old command prompt running the project.
- Run start.bat again.
- Make sure the black server window stays open.
- Open http://localhost:3000 manually.
- If start.bat says Java was not found, install JDK 17+.

The server listens on port 3000 and binds to the local machine.
