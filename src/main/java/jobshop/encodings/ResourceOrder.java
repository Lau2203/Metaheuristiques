package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.IntStream;

/* TRAVAIL AVANT MISE A JOUR DU CODE RENTREE VACANCES
public class ResourceOrder {
    public Task[][] matriceTask;
    public Instance instance;
    public Schedule schedule;



    //CONSTRUCTEUR
    public ResourceOrder (Instance instance) {
        this.instance = instance;

        this.matriceTask = new Task[this.instance.numMachines][this.instance.numJobs];
        for (int machine = 0; machine < this.instance.numMachines; machine++) {
            for (int job = 0; job < this.instance.numJobs; job++) {
                matriceTask[machine][job] = new Task(-1, -1);
            }
        }
    }



    //FROM SCHEDULE - FACULTATIF
    public void fromSchedule(Instance instance, Schedule schedule) {
        this.instance = instance;
        this.schedule = schedule;
        this.matriceTask = new Task[instance.numMachines][instance.numJobs];

        for (int machine = 0; machine < this.instance.numMachines; machine++) {
            for (int job = 0; job < this.instance.numMachines; job++) {
                matriceTask[machine][job] = new Task(job, this.instance.task_with_machine(job, machine));

            }
            sortByTimeMatrix(matriceTask,machine);
        }
    }




    //BUBLE_SORT POUR QUE LES TACHES SOIENT RANGEES SUIVANT LEUR MOMENT DE DEBUT
    public void sortByTimeMatrix(Task[][] matriceTask, int machine){
            int n = this.instance.numJobs;
            for (int i = 0; i < n-1; i++) {
                for (int j = 0; j < n-i-1; j++) {
                    //Si le moment de départ de la tâche J+1 et < à celui de la tâche J, alors échange
                    if (schedule.startTime(j + 1, matriceTask[machine][j + 1].task) < schedule.startTime(j, matriceTask[machine][j].task)) {
                        Task matriceTemporaire = matriceTask[machine][j];
                        matriceTask[machine][j] = matriceTask[machine][j + 1];
                        matriceTask[machine][j + 1] = matriceTemporaire;
                    }
                }
            }
    }


    //TO SCHEDULE - ASSOCIE A CHAQUE TACHE UNE DATE DE DEBUT
    public Schedule toSchedule() {

        //Time at which each machine is going to be freed
        int[] nextFreeTimeResource = new int[instance.numMachines];
        ArrayList<Task> tabTasksScheduled = new ArrayList<>();
        ArrayList<Task> executableTaskResult;
        //Le moment de départ de chaque tâche
        int[][] startTime = new int[this.instance.numJobs][this.instance.numTasks];

        int[] jobsTab = new int[this.instance.numJobs*this.instance.numMachines];
        for (int i = 0; i < jobsTab.length ; i++) {
            jobsTab[i] = -1;
        }

        for (int i = 0 ; i < jobsTab.length ; i++ ) {

            //Appel de la méthode pour récupérer les tâches exécutables
            executableTaskResult = executableTask(tabTasksScheduled);

            //A DECLARER AVANT SI POSSIBLE
            //on récupère la première tâche de l'Array qui est non-exécutable
            Task firstTaskExec = executableTaskResult.get(0);

            //on récupère le numéro de la machine associée
            int machine = instance.machine(firstTaskExec.job, firstTaskExec.task);

            // earliest start time for this firstTaskExec.task
            int debut = firstTaskExec.task == 0 ? 0 : startTime[firstTaskExec.job][firstTaskExec.task-1] + instance.duration(firstTaskExec.job, firstTaskExec.task-1);
            debut = Math.max(debut, nextFreeTimeResource[machine]);

            startTime[firstTaskExec.job][firstTaskExec.task] = debut;
            nextFreeTimeResource[machine] = debut + instance.duration(firstTaskExec.job, firstTaskExec.task);

            //task scheduled
            tabTasksScheduled.add(firstTaskExec);
        }
        return new Schedule(instance, startTime);
    }

    private ArrayList<Task> executableTask(ArrayList<Task> tabTasksScheduled) {
        ArrayList<Task> executableTask = new ArrayList<Task>();
        for (int machine = 0; machine < instance.numMachines; machine++) {
            for (int job = 0; job < instance.numJobs; job++) {
                //tout d'abord, il faut que la tâche soit nonScheduled
                if (!(tabTasksScheduled.contains(matriceTask[machine][job]))) {
                    //regardons son prédécesseur
                    if ((matriceTask[machine][job].task == 0) || (tabTasksScheduled.contains(new Task(matriceTask[machine][job].job, matriceTask[machine][job].task - 1)))) {
                        //mais est-ce la première tâche de la machine? sinon regarder le précédent de la même machine
                        if ((job == 0) || tabTasksScheduled.contains(matriceTask[machine][job - 1])) {
                            executableTask.add(matriceTask[machine][job]);
                        }
                    }
                }
            }
        }
        return executableTask;
    }


    //TO STRING POUR AFFICHAGE
    @Override
    public String toString() {
        String resultat = "";
        for(int machine =0; machine < this.instance.numMachines; machine++){
            resultat += ("Machine " + machine + " : ");
            for(int job = 0; job < this.instance.numJobs; job ++){
                resultat += (matriceTask[machine][job] + " ");
            }
            resultat += "\n";
        }
        return resultat;
    }

}
*/

public class ResourceOrder extends Encoding {

    // for each machine m, taskByMachine[m] is an array of tasks to be
    // executed on this machine in the same order
    public final Task[][] tasksByMachine;

    // for each machine, indicate on many tasks have been initialized
    public final int[] nextFreeSlot;

    /** Creates a new empty resource order. */
    public ResourceOrder(Instance instance)
    {
        super(instance);

        // matrix of null elements (null is the default value of objects)
        tasksByMachine = new Task[instance.numMachines][instance.numJobs];

        // no task scheduled on any machine (0 is the default value)
        nextFreeSlot = new int[instance.numMachines];
    }

    /** Creates a resource order from a schedule. */
    public ResourceOrder(Schedule schedule)
    {
        super(schedule.pb);
        Instance pb = schedule.pb;

        this.tasksByMachine = new Task[pb.numMachines][];
        this.nextFreeSlot = new int[instance.numMachines];

        for(int m = 0 ; m<schedule.pb.numMachines ; m++) {
            final int machine = m;

            // for thi machine, find all tasks that are executed on it and sort them by their start time
            tasksByMachine[m] =
                    IntStream.range(0, pb.numJobs) // all job numbers
                            .mapToObj(j -> new Task(j, pb.task_with_machine(j, machine))) // all tasks on this machine (one per job)
                            .sorted(Comparator.comparing(t -> schedule.startTime(t.job, t.task))) // sorted by start time
                            .toArray(Task[]::new); // as new array and store in tasksByMachine

            // indicate that all tasks have been initialized for machine m
            nextFreeSlot[m] = instance.numJobs;
        }
    }

    @Override
    public Schedule toSchedule() {
        // indicate for each task that have been scheduled, its start time
        int [][] startTimes = new int [instance.numJobs][instance.numTasks];

        // for each job, how many tasks have been scheduled (0 initially)
        int[] nextToScheduleByJob = new int[instance.numJobs];

        // for each machine, how many tasks have been scheduled (0 initially)
        int[] nextToScheduleByMachine = new int[instance.numMachines];

        // for each machine, earliest time at which the machine can be used
        int[] releaseTimeOfMachine = new int[instance.numMachines];


        // loop while there remains a job that has unscheduled tasks
        while(IntStream.range(0, instance.numJobs).anyMatch(m -> nextToScheduleByJob[m] < instance.numTasks)) {

            // selects a task that has noun scheduled predecessor on its job and machine :
            //  - it is the next to be schedule on a machine
            //  - it is the next to be scheduled on its job
            // if there is no such task, we have cyclic dependency and the solution is invalid
            Optional<Task> schedulable =
                    IntStream.range(0, instance.numMachines) // all machines ...
                            .filter(m -> nextToScheduleByMachine[m] < instance.numJobs) // ... with unscheduled jobs
                            .mapToObj(m -> this.tasksByMachine[m][nextToScheduleByMachine[m]]) // tasks that are next to schedule on a machine ...
                            .filter(task -> task.task == nextToScheduleByJob[task.job])  // ... and on their job
                            .findFirst(); // select the first one if any
            if(schedulable.isPresent()) {
                // we found a schedulable task, lets call it t
                Task t = schedulable.get();
                int machine = instance.machine(t.job, t.task);

                // compute the earliest start time (est) of the task
                int est = t.task == 0 ? 0 : startTimes[t.job][t.task-1] + instance.duration(t.job, t.task-1);
                est = Math.max(est, releaseTimeOfMachine[instance.machine(t)]);
                startTimes[t.job][t.task] = est;

                // mark the task as scheduled
                nextToScheduleByJob[t.job]++;
                nextToScheduleByMachine[machine]++;
                // increase the release time of the machine
                releaseTimeOfMachine[machine] = est + instance.duration(t.job, t.task);
            } else {
                // no tasks are schedulable, there is no solution for this resource ordering
                return null;
            }
        }
        // we exited the loop : all tasks have been scheduled successfully
        return new Schedule(instance, startTimes);
    }

    /** Creates an exact copy of this resource order. */
    public ResourceOrder copy() {
        return new ResourceOrder(this.toSchedule());
    }

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();
        for(int m=0; m < instance.numMachines; m++)
        {
            s.append("Machine ").append(m).append(" : ");
            for(int j=0; j<instance.numJobs; j++)
            {
                s.append(tasksByMachine[m][j]).append(" ; ");
            }
            s.append("\n");
        }

        return s.toString();
    }

}
