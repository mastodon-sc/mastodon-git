# Tutorial: How to share a project?
**(With Mastodon Git Collaboration Tools)**

## 0. Preparations

You need to have Fiji and the Mastodon Git collaboration tools installed. Here are the [installation instructions](installation.md).

## 1. Create Empty Repository at GitLab

In your browser open: https://git.mpi-cbg.de

![image](images/image12.png)

Click “Sign in”

![image](images/image8.png)

You will see a list of project. Please click “New project” button in the top right corner:

![image](images/image20.png)

Next click “Create blank project”:

![image](images/image11.png)

Provide project name and maybe a description. Then click “Create project”

![image](images/image10.png)

You will now see the landing page of your newly created git repository:

![image](images/image31.png)

Next click “Clone” and copy the “https://git….” URL

![image](images/image29.png)

This URL will later be used to share the project from within Mastodon.

## 2. Upload Mastodon Project to GitLab

Open your Mastodon project on your computer with Fiji

![image](images/image9.png)

In the Mastodon menu click “Plugins > Git > Initialize > Share Project”

![image](images/image26.png)

A window will open. In this window provide the URL that we copied at the end of step 1. And

![image](images/image22.png)

You will be asked for username and password

![image](images/image14.png)

The mastodon project has now been uploaded to the GitLab server you can verify by going to the the git repositories landing page. In our case:

![image](images/image28.png)

https://git.mpi-cbg.de/arzt/trackathon-lyon
(The git repository is private. You can only access the landing page, if you are invited to the repository.)

You should see a entry “mastodon.project”
