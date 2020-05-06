package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Schedule;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {

            Task stockage = order.tasksByMachine[this.machine][t1];
            order.tasksByMachine[this.machine][t1] = order.tasksByMachine[this.machine][t2];
            order.tasksByMachine[this.machine][t2] = stockage;

        }
    }


    @Override
    public Result solve(Instance instance, long deadline) {
        //enumPriority dans Gloutonne a du passer static pour faire ça
        //on se sert du solver SPT (ou autre) pour trouver la meilleure solution à partir de lui
        Schedule bestSolution = new Gloutonne(Gloutonne.enumPriority.EST_LRPT).solve(instance, deadline).schedule;

        List<Block> blocks = blocksOfCriticalPath(new ResourceOrder(bestSolution));

        boolean aucuneAmélioration = false;
        ResourceOrder bestVoisin = null;

        while (!aucuneAmélioration && deadline - System.currentTimeMillis() > 1) {
            //recherche du meilleur voisin
            for (int block = 0; block < blocks.size(); block++) {
                List<Swap> voisins = neighbors(blocks.get(block));
                for (Swap swap : voisins) {
                    // voisin de la meilleure solution
                    ResourceOrder voisin = new ResourceOrder(bestSolution);
                    swap.applyOn(voisin);
                    Schedule voisinSched = voisin.toSchedule();
                    aucuneAmélioration = true;
                    // si l'objectif est meilleur
                    if (bestVoisin == null) {
                        bestVoisin = voisin.copy();
                    } else {
                        if (voisinSched.makespan() < bestVoisin.toSchedule().makespan()) {
                            bestVoisin = voisin.copy();
                        }
                    }
                }
                // fin de parcours des voisins
                if (bestVoisin.toSchedule().makespan() < bestSolution.makespan()) {
                    bestSolution = bestVoisin.toSchedule();
                    bestVoisin = null;
                    aucuneAmélioration = false;

                }
            }
       }
        return new Result(instance, bestSolution, Result.ExitCause.Timeout);
    }



    /** Returns a list of all blocks of the critical path. */
    static List<Block> blocksOfCriticalPath(ResourceOrder order) {

        ArrayList<Block> blocksList = new ArrayList<Block>();
        List<Task> criticalPath = order.toSchedule().criticalPath();

        Task taskCriticalPath = criticalPath.get(0);
        Task taskDebut = new Task(taskCriticalPath.job,taskCriticalPath.task);

        
        int numMachine = order.instance.machine(taskCriticalPath.job,taskCriticalPath.task);
        int nombreTask = 1;

        for (int i = 1; i < criticalPath.size(); i++){
            taskCriticalPath = criticalPath.get(i);
            //tache suivante même machine
            if(numMachine == order.instance.machine(taskCriticalPath.job,taskCriticalPath.task)){
                nombreTask++;
            }else{
                // pas même machine mais min 2 tâches consécutives
                if(nombreTask >= 2){
                    int nbTacheDebut = 0;

                    for(int j = 0; j < order.tasksByMachine[numMachine].length; j++){
                        if(taskDebut.equals(order.tasksByMachine[numMachine][j])){
                            nbTacheDebut = j;
                        }
                    }
                    int tacheFin = nbTacheDebut + nombreTask - 1;
                    blocksList.add(new Block(numMachine,nbTacheDebut,tacheFin));
                }
                nombreTask = 1;
                taskDebut = new Task(taskCriticalPath.job,taskCriticalPath.task);
                numMachine = order.instance.machine(taskCriticalPath.job,taskCriticalPath.task);
            }
        }
        // fin du parcours du chemin critique
        // il y avait au moins 2 taches consecutives dedans
        if(nombreTask >= 2){
            int tacheDebut = 0;

            for(int j = 0; j < order.tasksByMachine[numMachine].length; j++){
                if(taskDebut.equals(order.tasksByMachine[numMachine][j])){
                    tacheDebut = j;
                }
            }
            int tacheFin = tacheDebut + nombreTask - 1;
            blocksList.add(new Block(numMachine,tacheDebut,tacheFin));
        }
        return blocksList;
    }



    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {

        List<Swap> listeVoisins = new ArrayList<Swap>();

            listeVoisins.add(new Swap(block.machine, block.firstTask,	block.firstTask + 1));
            listeVoisins.add(new Swap(block.machine, block.lastTask, 	block.lastTask - 1));

        return listeVoisins;
    }


}