/** Reese Butler
 *  6/15/2011
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>
#include <signal.h>
#include <sys/stat.h>
#include <linux/input.h>

int sockfd, newsockfd, portnumber = 4444, client_length, n;
char buffer[256]; //The character buffer used to grab the inputs
struct sockaddr_in serv_addr, cli_addr; //Store the server and client addresses
float x, y, z, pitch, yaw, roll = 0.0; //Store the input values
int fifo;
char *pipe_name = "event12"; //The name of the pipe

//Handles error messages
void error(char *msg)
{
	perror(msg);
	exit(1);
}

//writes the provided value to the specified axis
void write_value(int axis, float value)
{
	struct input_event event;
	
	event.code = axis;
	event.value = value;
	event.type = EV_REL;
	gettimeofday(&event.time, NULL);
	
	write(fifo, &event, sizeof(event));
}

//Runs the server
void start_server(void)
{	
	int first = 1;
	sockfd = socket(AF_INET, SOCK_STREAM, 0);
	if(sockfd < 0)
		error("ERROR opening socket");
		
	bzero((char *) &serv_addr, sizeof(serv_addr)); //sets all values in the buffer to zero
	
	//Initializes the values of the server address
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_port = htons(portnumber);
	serv_addr.sin_addr.s_addr = INADDR_ANY;
	
	if(bind(sockfd, (struct sockaddr *) &serv_addr, sizeof(serv_addr)) < 0)
		error("ERROR on binding");
		
	listen(sockfd, 5);
	
	client_length = sizeof(cli_addr);
	printf("waiting for client connection...\n");
	
	while(1)
	{	
		newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &client_length); //Waits for the connection with a client
	
		if(newsockfd < 0)
			error("ERROR on accept");
	
		//splits into 2 processes
		int pid = fork();
		
		if(pid == 0) //executed by child process
		{
			close(sockfd);
			
			while(1)
			{
				n = read(newsockfd, buffer, 255);
				
				if(n <= 0)
				{
					printf("connection lost\n");
					printf("waiting for client connection...\n");
					first = 1;
					exit(0);
				}
				else
				{
					if(first)
					{
						printf("connected successfully!\n");
						first = 0;
					}
					sscanf(buffer, "%f,%f,%f,%f,%f,%f", &yaw, &pitch, &roll, &x, &y, &z);
					
					write_value(0, x);
					write_value(1, y);
					write_value(2, z);
					write_value(3, pitch);
					write_value(4, yaw);
					write_value(5, roll);
				}
			}
		}
		else //executed by parent process
			close(newsockfd);
	}
}

//Called when the program end (most likely when the user presses ctrl+c)
void quit(int msg)
{
	signal(SIGINT, SIG_DFL);
	printf("\nexiting...\n");
	exit(0);
}

int main(int argc, char* argv[])
{ 
	if(argc != 3)
	{
		printf("Usage: %s port# fifo\n", argv[0]);
		printf("Use \"-\" for default values (port# = 4444; fifo = event12)\n\n");
		close(fifo);
		exit(1);
	}
	
	//initializes port number from the commandline
	if(strcmp("-", argv[1]) != 0)
		portnumber = atoi(argv[1]); 
	
	//initializes the pipe directory (name)
	if(strcmp("-", argv[2]) != 0)
		pipe_name = argv[2]; 

	fifo = open(pipe_name, O_WRONLY); //Opens the named pipe
	
	if(fifo < 0)
		error("ERROR opening pipe");

	signal(SIGINT, quit); //Handles process interruption
	start_server(); //Begins the server process which waits for connections
	return 0;
}
