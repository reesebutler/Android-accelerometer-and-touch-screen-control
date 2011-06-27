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
int newx, newy, newz, newpitch, newyaw, newroll = 0;
int fifo, vflag = 0;
int counter = 0;
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
	event.type = EV_ABS;
	gettimeofday(&event.time, NULL);
	
	write(fifo, &event, sizeof(event));
}

//Prints values for debugging
void print_verbose()
{
	printf("%10f %10f %10f %10f %10f %10f\n", x, y, z, pitch, yaw, roll);
}

//Normalizes the input values
float normalize_value(int axis, float value)
{
	if(axis == 0 || axis == 1)
		return value * -15;
	else if(axis == 2)
		return value * 40;
	else if(axis == 3 && value != 0)
		return value * -4;
	else if(axis == 4)
		return value * 4;
	else if(axis == 5)
		return value * 80;
	return 0;
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
				
				if(n <= 0) //If the read fails
				{
					printf("connection lost\n");
					printf("waiting for client connection...\n");
					first = 1;
					exit(0);
				}
				else //If the read is successful
				{
					if(first)
					{
						printf("connected successfully!\n");
						first = 0;
						counter = 0;
					}
					sscanf(buffer, "%f,%f,%f,%f,%f,%f", &yaw, &pitch, &roll, &x, &y, &z);
					
					newx = (int)x;
					newy = (int)y;
					newz = (int)z;
					newpitch = (int)pitch;
					newyaw = (int)yaw;
					newroll = (int)roll;
					
					x = normalize_value(0, x);
					y = normalize_value(1, y);
					z = normalize_value(2, z);
					pitch = normalize_value(3, pitch);
					roll = normalize_value(4, roll);
					yaw = normalize_value(5, yaw);
					
					write_value(0, x); //x
					write_value(1, y); //y
					write_value(2, z); //z
					write_value(3, pitch); //pitch
					//write_value(4, yaw); //roll
					write_value(5, roll); //yaw
					
					if(vflag)
						print_verbose();
	
					write(newsockfd,"I got your message\n",19);
				}
			}
		}
		else //executed by parent process
			close(newsockfd);
	}
}

//Called when the program ends (most likely when the user presses ctrl+c)
void quit(int msg)
{
	signal(SIGINT, SIG_DFL);
	printf("\nexiting...\n");
	close(newsockfd);
	close(fifo);
	exit(0);
}

int main(int argc, char* argv[])
{ 

	int c, hflag = 0;
	
	//Parses command-line arguments
	while((c = getopt(argc, argv, "p:f:hv")) != -1)
	{
		switch(c)
		{
			case 'p':
				portnumber = atoi(optarg);
				break;
			case 'f':
				pipe_name = optarg;
				break;
			case 'h':
				hflag = 1;
				break;
			case 'v':
				vflag = 1;
				break;
		}
	}
	
	//Prints the usage
	if(hflag)
	{
		printf("Usage: %s [-p <port>] [-f <pipe_name>]\n\n", argv[0]);
		exit(0);
	}
	
	printf("waiting for Google Earth to open other end of pipe...\n");
	fifo = open(pipe_name, O_WRONLY); //Opens the named pipe
	
	if(fifo < 0)
		error("ERROR opening pipe");

	signal(SIGINT, quit); //Handles process interruption
	start_server(); //Begins the server process which waits for connections
	return 0;
}
