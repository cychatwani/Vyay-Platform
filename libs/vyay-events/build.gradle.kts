plugins {
    `java-library`
}

group = "com.vyay"
version = "0.0.1"

// Deliberately ZERO dependencies.
//
// Both sides of the wire compile against this module: the producer (services:core,
// a Spring Boot app on JPA) and the consumers (an Insights service, and whatever
// else subscribes later). A dependency added here is forced on all of them, so
// neither side's framework may leak in — no Spring, no Kafka, no JPA, not even a
// JSON annotation library. Serialization is the transport's problem; these types
// are plain records and enums that any reflective or record-aware mapper can read.
//
// The Java toolchain (21) comes from the root build's `subprojects` block. Records
// and sealed interfaces are the only language features used, so a consumer needs
// nothing beyond a Java 17+ runtime.
