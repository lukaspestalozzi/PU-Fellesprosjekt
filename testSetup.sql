

Drop database test_pu;
create database test_pu;
use test_pu;

CREATE TABLE Room (
    roomID        VARCHAR(10)    NOT NULL,
    size        INT        NOT NULL,
    PRIMARY KEY (roomID)
);
 
 

 
CREATE TABLE User (
    username    VARCHAR(30)    NOT NULL,
    name        VARCHAR(100)   NOT NULL,
    password    VARCHAR(20)    NOT NULL, 	# the hashed password and salt. h(pw+salt)
    salt        VARCHAR(30), 			# to safely store the password
    email       VARCHAR(50),
    PRIMARY KEY (username)
);

CREATE TABLE CalendarEntry (
    entryID    	BIGINT   	NOT NULL AUTO_INCREMENT,
    startTime  	TIMESTAMP    	NOT NULL,
    endTime     TIMESTAMP    	NOT NULL,
    location   	VARCHAR(20)	DEFAULT NULL,
    description VARCHAR(100)    NOT NULL,
    roomID    	VARCHAR(10) 	DEFAULT NULL,
    creator    	VARCHAR(30)    	NOT NULL,
    PRIMARY KEY (entryID),
    FOREIGN KEY (roomID) REFERENCES Room (roomID)
        ON UPDATE CASCADE,
    FOREIGN KEY (creator) REFERENCES User(username)
        ON UPDATE CASCADE
);

 
CREATE TABLE Invitation ( # To store the user-calendarEntry relation
    isGoing        BOOLEAN        DEFAULT NULL,
    isShowing    BOOLEAN        DEFAULT TRUE,
    username    VARCHAR(30)    NOT NULL,
    entryID        BIGINT        NOT NULL,
    PRIMARY KEY (username, entryID),
    FOREIGN KEY (username) REFERENCES User (username)
        ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (entryID) REFERENCES CalendarEntry (entryID)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Alarm(
    alarmTime 	TIMESTAMP 	NOT NULL,
    username    VARCHAR(30)    	NOT NULL,
    entryID     BIGINT        	NOT NULL,

PRIMARY KEY(username, entryID),
FOREIGN KEY(username) REFERENCES User(username)
        ON UPDATE CASCADE ON DELETE CASCADE,
 FOREIGN KEY (entryID) REFERENCES CalendarEntry(entryID)
        ON UPDATE CASCADE ON DELETE CASCADE
);

#CREATE TRIGGER update_alarmTime
#AFTER UPDATE ON CalendarEntry
#FOR EACH ROW
#	UPDATE Alarm AS A 
#	SET A.alarmTime = NEW.startTime - (OLD.startTime - A.alarmTime)
#	WHERE A.entryID = NEW.entryID ;



CREATE TABLE Notification (
    notificationID BIGINT NOT NULL     AUTO_INCREMENT,
    description    VARCHAR(100)    NOT NULL,
    isOpened    BOOLEAN        DEFAULT FALSE,
    time        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP, # Time of creation
    username    VARCHAR(30)    NOT NULL,
    entryID        BIGINT        NOT NULL,
    PRIMARY KEY(notificationID),
    FOREIGN KEY(username) REFERENCES User(username)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (entryID) REFERENCES CalendarEntry(entryID)
        ON UPDATE CASCADE ON DELETE CASCADE
);
 
CREATE TABLE IsAdmin (
    entryID        BIGINT        NOT NULL,
    username    VARCHAR(30)    NOT NULL,
    PRIMARY KEY (entryID, username),
    FOREIGN KEY (entryID) REFERENCES CalendarEntry(entryID)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (username) REFERENCES User(username)
        ON UPDATE CASCADE ON DELETE CASCADE
);
 
CREATE TABLE Gruppe (
    groupname    VARCHAR(100)    NOT NULL,
    PRIMARY KEY (groupname)
);
 
CREATE TABLE MemberOf (
    groupname  VARCHAR(100)    NOT NULL,
    username    VARCHAR(30)    NOT NULL,
    PRIMARY KEY (groupname, username),
    FOREIGN KEY (groupname) REFERENCES Gruppe(groupname)
        ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (username) REFERENCES User(username)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE RoomReservation (
	roomID        	VARCHAR(10)    	NOT NULL,
	startTime	TIMESTAMP 	NOT NULL,
	endTime		TIMESTAMP 	NOT NULL,
	entryID        	BIGINT        	NOT NULL,
	PRIMARY KEY (entryID),
	FOREIGN KEY (roomID) REFERENCES Room(roomID) ON UPDATE CASCADE ON DELETE CASCADE,
	FOREIGN KEY (entryID) REFERENCES CalendarEntry(entryID) ON UPDATE CASCADE ON DELETE CASCADE
);


INSERT INTO Room (roomID, size)
VALUES ("K5-208", 8);

INSERT INTO User (username, name, password, email)
VALUES ("lukasap", "Lukas Pestalozzi", "1234", "lukasap@stud.ntnu.no");

INSERT INTO CalendarEntry (startTime, endTime, location, description, roomID, creator)
VALUES ('2015-03-03 10:33:17', '2015-03-03 13:33:17', "Gloeshaugen", "Database fellesprosjekt", "K5-208", "lukasap");

