#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include <X11/Xlib.h>

#define BUFF_SIZE   64

int main(int argc, char **argv) {

  char * ip_addr = NULL;
  char * ip_port = NULL;

  opterr = 0;
  int c;

  int phoneSocketFd;
  struct sockaddr_in phoneSocketAddr;

  Display* display;
  int screen;
  Window root_window;
  int screen_w, screen_h;

  char buffer[BUFF_SIZE];

  // parse arguments
  while ((c = getopt (argc, argv, "a:p:")) != -1) {
    if (c == 'a') {
      ip_addr = optarg;
    }
    if (c == 'p') {
      ip_port = optarg;
    }
    if (c == '?') {
      printf("wrong format\r\n");
    }
  }

  if (ip_addr == NULL || ip_port == NULL) {
    printf("missing arguments\r\n");
    return 1;
  }

  // init X11 stuff
  display = XOpenDisplay(0);
  screen = XDefaultScreen(display);
  root_window = XRootWindow(display, screen);

	screen_w = DisplayWidth(display, screen);
  screen_h = DisplayHeight(display, screen);
  
	printf("Screen size: %dx%d\n", screen_w, screen_h);

  // initialize address structure
  memset(&phoneSocketAddr, 0, sizeof(phoneSocketAddr));
  phoneSocketAddr.sin_family = AF_INET;
  phoneSocketAddr.sin_port = htons(atoi(ip_port));
  if (phoneSocketAddr.sin_port == 0) {
    printf("wrong port number\n");
    return 1;
  }
  if (1 != inet_pton(AF_INET, ip_addr, &phoneSocketAddr.sin_addr)) {
    printf("wrong address\n");
    return 1;
  }

  // open socket and connect
  phoneSocketFd = socket(AF_INET, SOCK_DGRAM, 0);
  if (phoneSocketFd < 0) {
    printf("error opening socket\n");
    return 1;
  }

  // if (bind(phoneSocketFd, (struct sockaddr *) &phoneSocketAddr, sizeof(phoneSocketAddr)) < 0) {
  //   printf("error connecting to phone\n");
  //   return 1;
  // } 

  // send something (let app know ip:port of this machine)
  if (sendto(phoneSocketFd, buffer, 1, 0, (struct sockaddr*) &phoneSocketAddr, sizeof(phoneSocketAddr)) == -1) {
    printf("error sending data\n");
    return 1;
  }

  int addrLen = sizeof(phoneSocketAddr);

  int end = 0;
  while(!end) {
    memset(buffer, '\0', BUFF_SIZE);

    int recv_len = recvfrom(
      phoneSocketFd,
      buffer,
      BUFF_SIZE,
      0,
      (struct sockaddr *) &phoneSocketAddr,
      &addrLen
    );

    int x;
    int y;

    // parse received data
    char * pch = strtok (buffer, ":");
    x = atoi(pch);
    pch = strtok (NULL, ":");
    y = atoi(pch);
    
    // set cursors position
    XWarpPointer(display, root_window, None, 0, 0, screen_w, screen_h, x, y);
    XFlush(display);

    // printf("here you go: %d:%d\n", x, y);
  }
  return 0;
}