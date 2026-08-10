package com.migration.manatal.sandbox;

public final class SandboxJobSample {

    private SandboxJobSample() {
    }

    public static final String JOB_DESCRIPTION = """
            Syffer is an all-inclusive consulting company focused on talent, tech and innovation. We exist to elevate companies and humans all around the world, making change, from the inside to the outside.

            We believe that technology + human kindness positively impacts every community around the world. Our approach is simple, we see a world without borders, and believe in equal opportunities. We are guided by our core principles of spreading positivity, good energy and promote equality and care for others.

            Our hiring process is unique! People are selected by their value, education, talent and personality. We dont present ethnicity, religion, national origin, age, gender, sexual orientation or identity.

            Its time to burst the bubble, and we will do it together!

            What You'll do:

            - Develop and maintain Java-based microservices supporting digital payment solutions;

            - Design, implement, and consume REST and gRPC APIs;

            - Build event-driven applications using Apache Kafka;

            - Migrate applications from on-premises infrastructure to public cloud environments;

            - Optimize application performance, scalability, resilience, and availability;

            - Implement and maintain CI/CD pipelines for automated deployments;

            - Monitor production environments using Prometheus and Grafana;

            - Develop and maintain automated test suites;

            - Collaborate with cross-functional teams to deliver secure and reliable payment services;

            - Hybrid work model;


            Who You Are:

            - Strong experience with Java and Spring Boot;

            - Proven experience developing Microservices architectures;

            - Experience designing and implementing REST and/or gRPC APIs;

            - Hands-on experience with Apache Kafka;

            - Experience deploying applications on Pivotal Cloud Foundry (PCF) or similar cloud platforms;

            - Experience with CI/CD tools and automated deployment pipelines;

            - Hands-on experience with Prometheus and Grafana for monitoring and observability;

            - Experience with automated testing frameworks (e.g., JUnit, Mockito, Testcontainers);

            - Knowledge of distributed systems design, including scalability, fault tolerance, and resilience;

            - Experience developing and deploying applications in cloud environments (private and/or public);

            - Familiarity with containerized application deployment and cloud-native architectures;

            - Fluent in portuguese and english;


            What you'll get:

            - Wage according to candidate's professional experience;

            - Remote Work whenever possible;

            - Delivery of work equipment adjusted to the performance of functions;

            - Benefits plan;

            - And others.

            Work together with expert teams on projects of large magnitude and intensity, long term together with our clients, all leaders in their industries.

            Are you ready to step into a diverse and inclusive world with us?

            Together we will promote uniquess!""";

    public static final String SAMPLE_SOURCE_JOB = """
            {
              "position_name": "Mock Software Developer",
              "description": "__DESCRIPTION__",
              "headcount": 1,
              "organization": 3779409,
              "owner": 123,
              "status": "planning",
              "city": "Lisboa",
              "country": "Portugal",
              "state": "Porto",
              "open_at": "2026-08-01T09:00:00Z",
              "close_at": null,
              "contract_details": "full_time",
              "industry": { "id": 384181, "name": "Accounting / Audit / Tax Services" },
              "custom_fields": {
                "clientrate": "target rate: 220€-230€/dia",
                "costday": 20,
                "rateday": 1,
                "categoy": ["IT"],
                "portugus": ["Obrigatório"],
                "inherited": false,
                "jobmodel": "Hybrid",
                "atualstatus": "Active",
                "contactname": "7728214",
                "grossmargin": 30,
                "ratehistory": "taxa anterior: 150",
                "businessunit": "PT - IT",
                "englishlevel": "Advanced (C1,C2)",
                "startdatejob": "2026-08-10",
                "morethanmonth": true,
                "purchaseorder": "1",
                "activebusiness": true,
                "consultantname": "Consultor Teste",
                "headcountatual": 2,
                "officelocation": ["Lisboa"],
                "technicalskill": ["Java", "Spring"],
                "internalization": "After 12 Months",
                "startdatesyffer": "2026-08-15",
                "invoicepaymentterm": "30 Days",
                "firstjobclosedinclient": false,
                "experiencelevelofficial": ["Middle"],
                "replacepreviousposition": false,
                "jobmodeldetails": "Detalhe adicional do mock.",
                "projectnotes": "<p>Notas do projeto</p>",
                "lostreason": "Closed with another consultancy"
              }
            }
            """.replace("__DESCRIPTION__", JOB_DESCRIPTION.replace("\n", "\\n"));
}
