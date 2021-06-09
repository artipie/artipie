Everyweek, the owners of Artipie will host the weekly meeting using Zoom.  Anyone who is interested in the project is welcomed to join the Zoom meeting.  

Here is the meeting information: 
Zoom Meeting ID: 626 620 058
Time: Every Friday 9 - 10 UTC+03:00 Time

Agenda for 7/10/2020 9AM meeting:
1. Kirill will demo how to use central.artipie.com and explain the deployment in https://github.com/artipie/central/blob/master/docker-compose.yml 
> * Upload/download the java Maven project
> * Create a new user and reset password for an existing user 
> * Check the logs of central site if errors happens
> * Create another repo for Golang or just any binary 
2. After setting up a local container of Artipie in the mac, how can we do the tasks above?  
3. Discuss the deployment strategy for Artipie how to scale out.  
> * How can we add the second container of Artipie to form a cluster, in order to support high availability and high scalability?  Kubernetes orchestration is always a good way here. 
> * Separate dashboard to a different project repo or package it to a different container 

Future topics: 
1. Use Kubernetes Helm instead of Docker compose? 
2. How does Java reactivex programming model work for Artipie? 
