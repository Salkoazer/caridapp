CREATE TABLE registered_user (
    userID INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(18) UNIQUE NOT NULL,
    pass_word VARCHAR(18) NOT NULL
);

CREATE TABLE theme (
    id_theme INTEGER PRIMARY KEY AUTOINCREMENT,
    theme_Name VARCHAR(40) UNIQUE NOT NULL
);

CREATE TABLE project (
    projectID INTEGER PRIMARY KEY AUTOINCREMENT,
    title VARCHAR(40) UNIQUE NOT NULL,
    summary VARCHAR(400) NOT NULL,
    image VARCHAR(200) NOT NULL,
    id_theme INTEGER NOT NULL
);

INSERT INTO registered_user VALUES(
1,
"joao",
"1234"
);

INSERT INTO registered_user VALUES(
2,
"vitor",
"1234"
);

INSERT INTO theme(theme_Name) VALUES
("Animal Welfare"),
("Child Protection"),
("Climate Action"),
("Peace and Reconciliation"),
("Disaster Response"),
("Economic Growth"),
("Education"),
("Ecosystem Restoration"),
("Gender Equality"),
("Physical Health"),
("Ending Human Trafficking"),
("Justice and Human Rights"),
("Sport"),
("Digital Literacy"),
("Food Security"),
("Arts and Culture"),
("LGBTQIA+ Equality"),
("COVID-19"),
("Clean Water"),
("Disability Rights"),
("Ending Abuse"),
("Mental Health"),
("Racial Justice"),
("Refugee Rights"),
("Reproductive Health"),
("Safe Housing"),
("Sustainable Agriculture"),
("Wildlife Conservation");


INSERT INTO project VALUES(
1,
"Help Kenya's Only Community-Run Elephant Sanctuary",
"Reteti Elephant Sanctuary aims to reunite, rescue, 
rehabilitate and re-release orphaned and abandoned 
elephant calves from across the north Kenya landscape, 
whilst creating much needed benefits for more than 100 
local people. As a community-run project, Reteti requires 
ongoing funds for milk, medical care, rescues  and staff 
salaries, as we give injured, traumatised or abandoned 
elephants a second chance at life.",
"https://www.globalgiving.org/pfil/50042/pict_grid7.jpg",
28
);